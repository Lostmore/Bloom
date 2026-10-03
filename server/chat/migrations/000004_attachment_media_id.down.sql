ALTER TABLE message_attachments ADD COLUMN url TEXT;
ALTER TABLE message_attachments ADD COLUMN media_type VARCHAR(50);

UPDATE message_attachments
SET url = '/api/v1/media/' || media_id::text,
    media_type = 'unknown';

ALTER TABLE message_attachments ALTER COLUMN url SET NOT NULL;
ALTER TABLE message_attachments ALTER COLUMN media_type SET NOT NULL;

ALTER TABLE message_attachments DROP COLUMN media_id;
