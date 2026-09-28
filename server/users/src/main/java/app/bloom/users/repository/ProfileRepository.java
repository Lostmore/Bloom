package app.bloom.users.repository;

import app.bloom.users.model.Gender;
import app.bloom.users.model.Interest;
import app.bloom.users.model.Location;
import app.bloom.users.model.Preferences;
import app.bloom.users.model.Privacy;
import app.bloom.users.model.Profile;
import app.bloom.users.model.SearchMode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ProfileRepository {
    private final JdbcClient jdbc;
    private final ObjectMapper json;

    public ProfileRepository(JdbcClient jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public Optional<Profile> find(UUID id) {
        return jdbc.sql("SELECT * FROM profiles WHERE id = :id")
                .param("id", id).query(this::map).optional();
    }

    public Optional<Profile> lock(UUID id) {
        return jdbc.sql("SELECT * FROM profiles WHERE id = :id FOR UPDATE")
                .param("id", id).query(this::map).optional();
    }

    public List<Profile> findAll(Set<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return jdbc.sql("SELECT * FROM profiles WHERE id IN (:ids) ORDER BY id")
                .param("ids", ids).query(this::map).list();
    }

    public boolean create(UUID id, String nickname, LocalDate birthDate, Gender gender, Set<SearchMode> modes) {
        return jdbc.sql("""
                INSERT INTO profiles (id, nickname, birth_date, gender, search_modes, preferences, privacy)
                VALUES (:id, :nickname, :birthDate, :gender, :modes::jsonb, :preferences::jsonb, :privacy::jsonb)
                ON CONFLICT (id) DO NOTHING
                """)
                .param("id", id).param("nickname", nickname).param("birthDate", birthDate)
                .param("gender", gender.name()).param("modes", encode(modes))
                .param("preferences", encode(new Preferences(18, 99, 50, Set.of(Gender.values()), modes)))
                .param("privacy", encode(new Privacy(true, true, true))).update() == 1;
    }

    public void update(Profile profile) {
        jdbc.sql("""
                UPDATE profiles
                SET nickname = :nickname, gender = :gender, bio = :bio, city = :city,
                    relationship_goal = :goal, search_modes = :modes::jsonb, version = version + 1
                WHERE id = :id
                """)
                .param("id", profile.id()).param("nickname", profile.nickname())
                .param("gender", profile.gender().name()).param("bio", profile.bio()).param("city", profile.city())
                .param("goal", profile.relationshipGoal()).param("modes", encode(profile.searchModes())).update();
    }

    public void preferences(UUID id, Preferences value) {
        jdbc.sql("UPDATE profiles SET preferences = :value::jsonb, version = version + 1 WHERE id = :id")
                .param("id", id).param("value", encode(value)).update();
    }

    public void privacy(UUID id, Privacy value) {
        jdbc.sql("UPDATE profiles SET privacy = :value::jsonb, version = version + 1 WHERE id = :id")
                .param("id", id).param("value", encode(value)).update();
    }

    public void interests(UUID id, List<String> value) {
        jdbc.sql("UPDATE profiles SET interests = :value::jsonb, version = version + 1 WHERE id = :id")
                .param("id", id).param("value", encode(value)).update();
    }

    public void photos(UUID id, List<UUID> value) {
        jdbc.sql("UPDATE profiles SET photos = :value::jsonb, version = version + 1 WHERE id = :id")
                .param("id", id).param("value", encode(value)).update();
    }

    public void location(UUID id, Location value) {
        jdbc.sql("""
                UPDATE profiles SET latitude = :latitude, longitude = :longitude, version = version + 1
                WHERE id = :id
                """)
                .param("id", id).param("latitude", value == null ? null : value.latitude())
                .param("longitude", value == null ? null : value.longitude()).update();
    }

    public void heartbeat(UUID id) {
        jdbc.sql("""
                UPDATE profiles SET last_seen = now()
                WHERE id = :id AND (last_seen IS NULL OR last_seen < now() - interval '1 minute')
                """).param("id", id).update();
    }

    public void erase(UUID id) {
        jdbc.sql("""
                UPDATE profiles
                SET nickname = '', birth_date = NULL, gender = NULL, bio = '', city = '',
                    latitude = NULL, longitude = NULL, relationship_goal = '', search_modes = '[]',
                    preferences = '{}', privacy = '{}', interests = '[]', photos = '[]',
                    verified = FALSE, last_seen = NULL, deleted = TRUE, version = version + 1
                WHERE id = :id
                """).param("id", id).update();
        jdbc.sql("DELETE FROM user_blocks WHERE owner_id = :id OR target_id = :id").param("id", id).update();
        jdbc.sql("UPDATE reports SET description = '' WHERE reporter_id = :id").param("id", id).update();
    }

    public List<Interest> catalog() {
        return jdbc.sql("SELECT id, name, icon, category FROM interests ORDER BY id").query(Interest.class).list();
    }

    private Profile map(ResultSet row, int index) throws SQLException {
        Double latitude = row.getObject("latitude", Double.class);
        var seen = row.getTimestamp("last_seen");
        String gender = row.getString("gender");
        return new Profile(row.getObject("id", UUID.class), row.getString("nickname"),
                row.getObject("birth_date", LocalDate.class), gender == null ? null : Gender.valueOf(gender),
                row.getString("bio"), row.getString("city"),
                latitude == null ? null : new Location(latitude, row.getDouble("longitude")),
                row.getString("relationship_goal"), decode(row.getString("search_modes"), new TypeReference<>() {}),
                decode(row.getString("preferences"), new TypeReference<>() {}),
                decode(row.getString("privacy"), new TypeReference<>() {}),
                decode(row.getString("interests"), new TypeReference<>() {}),
                decode(row.getString("photos"), new TypeReference<>() {}), row.getBoolean("verified"),
                seen == null ? null : seen.toInstant(), row.getTimestamp("created_at").toInstant(),
                row.getLong("version"), row.getBoolean("deleted"));
    }

    private String encode(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize profile data", exception);
        }
    }

    private <T> T decode(String value, TypeReference<T> type) {
        try {
            return json.readValue(value, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Invalid stored profile data", exception);
        }
    }
}
