package postgres

import (
	"context"
	"testing"

	"github.com/stretchr/testify/require"
)

func TestRoomRepo_CreateAndGet(t *testing.T) {
	pool, teardown := setupTestDB(t)
	defer teardown()

	repo := NewRoomRepo(pool)
	ctx := context.Background()
	room, err := repo.CreateRoom(ctx, 100, 200)
	require.NoError(t, err)
	require.NotNil(t, room)
	require.NotZero(t, room.ID)
	require.Equal(t, int64(100), room.User1ID)
	require.Equal(t, int64(200), room.User2ID)
	require.True(t, room.IsActive)
	rooms1, err := repo.GetByUser(ctx, 100)
	require.NoError(t, err)
	require.Len(t, rooms1, 1)
	require.Equal(t, room.ID, rooms1[0].ID)
	rooms2, err := repo.GetByUser(ctx, 200)
	require.NoError(t, err)
	require.Len(t, rooms2, 1)
	require.Equal(t, room.ID, rooms2[0].ID)
	rooms3, err := repo.GetByUser(ctx, 999)
	require.NoError(t, err)
	require.Len(t, rooms3, 0)
}
