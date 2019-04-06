package sample;

public enum ResponseCode {
    ;

    public static final short USER_LOGGED_IN_PROCEED = 301;

    public static final short USER_LOGIN_NOT_SUCCUESSFUL = 302;

    public static final short USER_LOGGED_OUT = 401;

    public static final short USER_LOGOUT_UNSUCCESSFUL = 402;

    public static final short UPLOAD_SUCCESSFUL = 501;

    public static final short UPLOAD_NOT_SUCCESSFUL = 502;

    public static final short DOWNLOAD_SUCCESSFUL = 601;

    public static final short DOWNLOAD_UNSUCCESSFUL = 602;

    public static final short USER_FILES_LISTED = 701;

    public static final short USER_FILES_ERROR = 702;

    public static final short COMMAND_UNRECOGNIZED = 900;

    public static final short CANT_OPEN_DATA_CONNECTION = 425;



    public static final short REQUESTED_FILE_ACTION_NOT_TAKEN = 450;



    public static final short NOT_LOGGED_IN = 530;

    public static final short REQUESTED_ACTION_NOT_TAKEN = 550;
}
