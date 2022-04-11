import java.util.Objects;

public class Key {

    private final String embedTitle;
    private final Long userID;

    public Long getUserID() {
        return userID;
    }

    public String getEmbedTitle() {
        return embedTitle;
    }

    @Override
    public String toString() {
        return "Key{" +
                "embedTitle='" + embedTitle + '\'' +
                ", userID=" + userID +
                '}';
    }

    public Key(String embedTitle, Long userID) {
        this.embedTitle = embedTitle;
        this.userID = userID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Key)) return false;
        Key key = (Key) o;
        return Objects.equals(embedTitle, key.embedTitle) && Objects.equals(userID, key.userID);
    }

    @Override
    public int hashCode() {
        int result = embedTitle.hashCode();
        result = 31 * result + userID.hashCode();
        return result;
    }
}
