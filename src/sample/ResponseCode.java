package sample;

public enum ResponseCode {
    ;

    public static final short COMMAND_OKAY = 200;

    public static final short CLOSING_DATA_CONNECTION = 226;

    public static final short USER_LOGGED_IN_PROCEED = 230;

    public static final short USER_LOGGED_OUT_SERVICE_TERMINATED = 231;

    public static final short USERNAME_OK_NEED_PASSWORD = 331;

    public static final short CANT_OPEN_DATA_CONNECTION = 425;

    public static final short INVALID_USERNAME_OR_PASSWORD = 430;

    public static final short REQUESTED_FILE_ACTION_NOT_TAKEN = 450;

    public static final short SYNTAX_ERROR_COMMAND_UNRECOGNIZED = 500;

    public static final short NOT_LOGGED_IN = 530;

    public static final short REQUESTED_ACTION_NOT_TAKEN = 550;
}
