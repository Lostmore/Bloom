ALTER TABLE message_attachments DROP COLUMN media_id;
ALTER TABLE messages DROP CONSTRAINT uniq_sender_client_message;
ALTER TABLE messages DROP COLUMN client_message_id;