ALTER TABLE messages ADD COLUMN client_message_id UUID;
ALTER TABLE messages ADD CONSTRAINT uniq_sender_client_message UNIQUE (sender_id, client_message_id);
ALTER TABLE message_attachments ADD COLUMN media_id UUID;