# Границы Android ↔ backend

## Маршруты клиента

Все пути ниже относительно Gateway `/api/v1/`.

| Назначение | Запрос |
| --- | --- |
| Вход / регистрация | `POST auth/login`, `POST auth/register`: `{phoneNumber, password}` |
| Обновление / выход | `POST auth/refresh`, `POST auth/logout`: `{refreshToken}` |
| Своя анкета | `GET`, `PUT`, `PATCH users/me` |
| Чужая анкета | `GET users/{id}` |
| Интересы | `GET interests`, `PUT users/me/interests`: `{interests: [...]}` |
| Приватность | `GET`, `PUT users/me/privacy`: `{privacy: {...}}` |
| Реакция | `POST interactions/{id}/like`, `skip`, `super-interest`; UUID в `Idempotency-Key` |
| Совпадения | `GET matches?after=...`, `DELETE matches/{id}` |
| Go-комнаты | `GET`, `POST conversations`; создание: `{user2_id: UUID}` |
| Go-переписка | WebSocket `/api/v1/ws?room_id=число&token=ACCESS_TOKEN` |

`user1_id` клиент не отправляет: его определяет Chat через Identity. Служебные токены остаются только на сервере. Генератор `infrastructure/prepare-ci.py` заполняет `INTERNAL_TOKEN` для Chat/Media из сгенерированной конфигурации Identity. `IDENTITY_URL` уже задан в основном Compose.

## Что Go уже умеет и что использует приложение

`server/chat/cmd/main.go` регистрирует `/api/rooms` и `/ws`. Gateway переводит публичный `/conversations` в `/api/rooms`. `internal/pkg/auth/remote_validator.go` выполняет introspect. Не нужно писать ещё один механизм авторизации специально для Android.

WebSocket принимает `{ "content": "Привет" }`. История и новые сообщения приходят отдельными JSON-объектами с `id`, `room_id`, `sender_id`, `content`, `created_at`, `attachments`. Клиент отсеивает сообщения другой комнаты, дубликаты и некорректные кадры. В памяти экрана — последние 500 сообщений; серверной пагинации истории в текущем протоколе нет.

Сокет открыт только пока экран чата на переднем плане. При возврате открывается заново; перед окончанием access token клиент обновляет его и переподключается. Это временная совместимость с существующим handshake, а не новая серверная модель сессий. Сервер должен сам отзывать доступ; модифицированный клиент может не выполнять эти действия.

## Задачи Go-разработчику до публичного запуска

1. **Согласовать права Chat с Interactions.** В сервисе комнат проверять действующее совпадение/другую разрешённую причину общения. Подписаться на `match.created`, `match.closed` из `bloom.interactions.v1`; учитывать удаления/блокировки/отзыв сессий. Контракт: `contracts/interactions-events.md`, `contracts/identity-events.md`. Закрывать существующие WebSocket при отзыве доступа.
2. **Исправить обработку ошибок отправки.** `server/chat/internal/delivery/websocket/client.go`: при ошибке `SendMessage` сейчас код всё равно пишет в `Broadcast`. Возвращать клиенту ошибку и не публиковать несохранённое сообщение. Желателен `clientMessageId` + подтверждение, чтобы безопасно повторять отправку.
3. **Перенести токен из URL WebSocket.** Native Android умеет передавать `Authorization: Bearer …` при handshake. Сейчас Go читает query `token`, поэтому клиент соблюдает этот контракт. После поддержки заголовка заменить сборку URL в `ChatConnection.kt`; не писать query токена в access logs.
4. **Закрыть Media.** Текущий `GET /media/{id}` не проверяет получателя. Нужны назначение `PROFILE`/`CHAT`, владелец, связанная комната, проверка членства/прав при чтении, авторизованная выдача или короткоживущая подписанная ссылка. Ответ upload должен содержать стабильный `mediaId`. Для фото профиля реализовать существующий внутренний контракт Users → Media на проверку владельца/готовности/назначения фото. Только после этого включать фото и вложения на Android.
5. **Discovery.** Сервис сейчас заготовка. Android-адаптер предполагает `GET /discovery/feed?cursor=...` → `{items: [PublicProfile], nextCursor: string|null}`. Это **предложение**, не существующий согласованный контракт. Если Discovery возвращает список `userId`, адаптер нужно изменить: получить карточки из Users. Не добавлять фиктивных людей на сервер ради демонстрации.
6. **Activities и Notifications.** Серверные API, список активностей, создание/участие и push-токены ещё нужно согласовать. Клиент пока не изображает эти функции рабочими.

Go-файлы в рамках Android-задачи не изменялись. Можно запускать приложение сразу с готовыми Identity/Users/Interactions/Chat; пункты выше описывают оставшиеся ограничения, а не скрытые изменения протокола.
