INSERT INTO outbox_events (id, aggregate_id, event_type, payload)
SELECT event_id, id, 'user.profile.completed', jsonb_build_object(
    'eventId', event_id, 'schemaVersion', 1, 'type', 'user.profile.completed',
    'userId', id, 'version', version, 'occurredAt', now(), 'data', '{}'::jsonb)
FROM (SELECT gen_random_uuid() AS event_id, id, version FROM profiles
      WHERE NOT deleted AND length(trim(nickname)) > 0
        AND birth_date <= CURRENT_DATE - interval '18 years'
        AND birth_date >= CURRENT_DATE - interval '120 years'
        AND gender IS NOT NULL AND jsonb_array_length(search_modes) > 0) ready;
