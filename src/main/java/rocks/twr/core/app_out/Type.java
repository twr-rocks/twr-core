package rocks.twr.core.app_out;

public enum Type {
    NEW("new"),

    TRIGGER("trigger"),

    RETRY("retry"),

    COMPLETED("completed"),

    FAILED("failed"),

    IGNORE("ignore"),

    ADMIN_REMOVE_BY_ID("admin-remove-by-id"), // remove record with given id, for given key

    ADMIN_REMOVE_ALL("admin-remove-all"), // remove all records for a given key

    DATABASE_DROPPED("database-dropped"), // used for alerting the application that a drop was detected

    TABLE_DROPPED("table-dropped") // used for alerting the application that a drop was detected
    ;

    private String type;

    Type(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return type;
    }
}