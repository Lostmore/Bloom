package postgres

import (
	"context"
	"testing"

	"github.com/google/uuid"
	"github.com/stretchr/testify/require"
)

var (
	u100 = uuid.MustParse("00000000-0000-0000-0000-000000000100")
	u200 = uuid.MustParse("00000000-0000-0000-0000-000000000200")
	u999 = uuid.MustParse("00000000-0000-0000-0000-000000000999")
)

func TestRoomRepo_CreateAndGet(t *testing.T) {
	pool, teardown := setupTestDB(t)
	defer teardown()

	repo := NewRoomRepo(pool)
	ctx := context.Background()
	room, err := repo.CreateRoom(ctx, u100, u200)
	require.NoError(t, err)
	require.NotNil(t, room)
	require.NotZero(t, room.ID)
	require.Equal(t, u100, room.User1ID)
	require.Equal(t, u200, room.User2ID)
	require.True(t, room.IsActive)
	rooms1, err := repo.GetByUser(ctx, u100)
	require.NoError(t, err)
	require.Len(t, rooms1, 1)
	require.Equal(t, room.ID, rooms1[0].ID)
	rooms2, err := repo.GetByUser(ctx, u200)
	require.NoError(t, err)
	require.Len(t, rooms2, 1)
	require.Equal(t, room.ID, rooms2[0].ID)
	rooms3, err := repo.GetByUser(ctx, u999)
	require.NoError(t, err)
	require.Len(t, rooms3, 0)
}
