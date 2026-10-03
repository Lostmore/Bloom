-- Обновляем только те записи, где media_id еще пуст
UPDATE message_attachments 
SET media_id = right(url, 36)::uuid 
WHERE media_id IS NULL AND url LIKE '%/api/v1/media/%';

ALTER TABLE message_attachments ALTER COLUMN media_id SET NOT NULL;

ALTER TABLE message_attachments DROP COLUMN url;
ALTER TABLE message_attachments DROP COLUMN media_type;
