/**
 * <a href="http://www.openolat.org">
 * OpenOLAT - Online Learning and Training</a><br>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at the
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache homepage</a>
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Initial code contributed and copyrighted by<br>
 * frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.modules.reminder.manager;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.TypedQuery;

import org.olat.core.commons.persistence.DB;
import org.olat.core.commons.persistence.PersistenceHelper;
import org.olat.core.id.Identity;
import org.olat.modules.reminder.Reminder;
import org.olat.modules.reminder.SentReminder;
import org.olat.modules.reminder.model.ReminderImpl;
import org.olat.modules.reminder.model.SentReminderImpl;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryEntryRef;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 
 * Initial date: 01.04.2015<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
@Service
public class ReminderDAO {
	
	@Autowired
	private DB dbInstance;

	public Reminder createReminder(RepositoryEntry entry) {
		ReminderImpl reminder = new ReminderImpl();
		Date now = new Date();
		reminder.setCreationDate(now);
		reminder.setLastModified(now);
		reminder.setEntry(entry);
		return reminder;
	}

	public Reminder save(Reminder reminder) {
		Reminder mergedReminder;
		if(reminder.getKey() != null) {
			mergedReminder = dbInstance.getCurrentEntityManager().merge(reminder);
		} else {
			dbInstance.getCurrentEntityManager().persist(reminder);
			mergedReminder = reminder;
		}
		return mergedReminder;
	}

	public Reminder loadByKey(Long key) {
		List<Reminder> reminders = dbInstance.getCurrentEntityManager()
				.createNamedQuery("loadReminderByKey", Reminder.class)
				.setParameter("reminderKey", key)
				.getResultList();
		return reminders.isEmpty() ? null : reminders.get(0);
	}

	public Reminder duplicate(Reminder toCopy) {
		ReminderImpl reminder = new ReminderImpl();
		Date now = new Date();
		reminder.setCreationDate(now);
		reminder.setLastModified(now);
		if(toCopy.getEntry() != null) {
			RepositoryEntry entryRef = dbInstance.getCurrentEntityManager()
					.getReference(RepositoryEntry.class, toCopy.getEntry().getKey());
			reminder.setEntry(entryRef);
		}
		
		reminder.setDescription(toCopy.getDescription() + " (Copy)");
		reminder.setConfiguration(toCopy.getConfiguration());
		reminder.setEmailBody(toCopy.getEmailBody());
		dbInstance.getCurrentEntityManager().persist(reminder);
		return reminder;
	}

	public void delete(Reminder reminder) {
		ReminderImpl ref = dbInstance.getCurrentEntityManager()
				.getReference(ReminderImpl.class, reminder.getKey());
		dbInstance.getCurrentEntityManager().remove(ref);
	}
	
	public List<Reminder> getReminders() {
		String q = "select rem from reminder rem inner join rem.entry entry";
		return dbInstance.getCurrentEntityManager()
				.createQuery(q, Reminder.class)
				.getResultList();
	}

	public List<Reminder> getReminders(RepositoryEntryRef entry) {
		String q = "select rem from reminder rem inner join rem.entry entry where entry.key=:entryKey";
		return dbInstance.getCurrentEntityManager()
				.createQuery(q, Reminder.class)
				.setParameter("entryKey", entry.getKey())
				.getResultList();
	}

	public SentReminderImpl markAsSend(Reminder reminder, Identity identity, String status) {
		SentReminderImpl send = new SentReminderImpl();
		send.setCreationDate(new Date());
		send.setStatus(status);
		send.setReminder(reminder);
		send.setIdentity(identity);
		dbInstance.getCurrentEntityManager().persist(send);
		return send;
	}
	
	public List<SentReminder> getSendReminders(Reminder reminder) {
		String q = "select sent from sentreminder sent inner join fetch sent.identity ident where sent.reminder.key=:reminderKey";
		return dbInstance.getCurrentEntityManager()
				.createQuery(q, SentReminder.class)
				.setParameter("reminderKey", reminder.getKey())
				.getResultList();
	}
	
	public List<SentReminder> getSendReminders(RepositoryEntryRef entry) {
		String q = "select sent from sentreminder sent inner join fetch sent.reminder rem inner join fetch sent.identity ident where rem.entry.key=:entryKey";
		return dbInstance.getCurrentEntityManager()
				.createQuery(q, SentReminder.class)
				.setParameter("entryKey", entry.getKey())
				.getResultList();
	}
	
	public List<Long> getReminderRecipientKeys(Reminder reminder) {
		String q = "select sent.identity.key from sentreminder sent where sent.reminder.key=:reminderKey";
		return dbInstance.getCurrentEntityManager()
				.createQuery(q, Long.class)
				.setParameter("reminderKey", reminder.getKey())
				.getResultList();
	}
	

	public Map<Long,Date> getCourseEnrollmentDates(RepositoryEntryRef entry, List<Identity> identities) {
		if(identities == null || identities.isEmpty()) {
			return new HashMap<Long,Date>();
		}

		List<Long> identityKeys = PersistenceHelper.toKeys(identities);

		StringBuilder sb = new StringBuilder();
		sb.append("select membership.identity.key, membership.creationDate from ").append(RepositoryEntry.class.getName()).append(" as v ")
		  .append(" inner join v.groups as relGroup")
		  .append(" inner join relGroup.group as baseGroup")
		  .append(" inner join baseGroup.members as membership")
		  .append(" where v.key=:repoKey");

		Set<Long> identityKeySet = null;
		if(identityKeys.size() < 100) {
			sb.append(" and membership.identity.key in (:identityKeys)");
		} else {
			identityKeySet = new HashSet<Long>(identityKeys);
		}

		TypedQuery<Object[]> query = dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), Object[].class)
				.setParameter("repoKey", entry.getKey());
		if(identityKeys.size() < 100) {
			query.setParameter("identityKeys", identityKeys);
		}

		List<Object[]> infoList = query.getResultList();
		Map<Long,Date> dateMap = new HashMap<>();
		for(Object[] infos:infoList) {
			Long identityKey = (Long)infos[0];
			if(identityKeySet == null || identityKeySet.contains(identityKey)) {
				Date enrollmantDate = (Date)infos[1];
				dateMap.put(identityKey, enrollmantDate);
			}
		}
		return dateMap;
	}

}
