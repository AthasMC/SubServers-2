package net.ME1312.SubServers.Sync.Library.Version;

import net.ME1312.SubServers.Sync.Library.Util;

import java.io.Serializable;

/**
 * Version Class
 */
@SuppressWarnings("serial")
public class Version implements Serializable, Comparable<Version> {
    private final Version parent;
    private final VersionType type;
	private final String string;

	public enum VersionType {
	    PRE_ALPHA(0, "pa", "pre-alpha"),
        ALPHA(1, "a", "alpha"),
        PREVIEW(2, "pb", "preview"),
        PRE_BETA(2, "pb", "pre-beta"),
        BETA(3, "b", "beta"),
        PRE_RELEASE(4, "pr", "pre-release"),
        RELEASE(5, "r", "release");

	    private final int id;
	    private final String shortname, longname;
        VersionType(int id, String shortname, String longname) {
	        this.id = id;
	        this.shortname = shortname;
	        this.longname = longname;
        }
    }

    /**
     * Creates a Version
     *
     * @param string Version String
     */
	public Version(String string) {
	    this(VersionType.RELEASE, string);
	}

    /**
     * Creates a Version
     *
     * @param type Version Type
     * @param string Version String
     */
	public Version(VersionType type, String string) {
	    this(null, type, string);
    }

    /**
     * Creates a Version (Prepending the parent)
     *
     * @param parent Parent Version
     * @param string Version String
     */
    public Version(Version parent, String string) {
        this(parent, VersionType.RELEASE, string);
    }

    /**
     * Creates a Version (Prepending the parent)
     *
     * @param parent Parent Version
     * @param type Version Type
     * @param string Version String
     */
    public Version(Version parent, VersionType type, String string) {
        if (Util.isNull(string, type)) throw new NullPointerException();
        this.parent = parent;
        this.type = type;
        this.string = string;
    }

    /**
     * Creates a Version
     *
     * @param ints Version Numbers (Will be separated with dots)
     */
    public Version(int... ints) {
        this(VersionType.RELEASE, ints);
    }

    /**
     * Creates a Version
     *
     * @param type Version Type
     * @param ints Version Numbers (Will be separated with dots)
     */
    public Version(VersionType type, int... ints) {
        this(null, type, ints);
    }

    /**
     * Creates a Version (Prepending the parent)
     *
     * @param parent Parent Version
     * @param ints Version Numbers (Will be separated with dots)
     */
    public Version(Version parent, int... ints) {
        this(parent, VersionType.RELEASE, ints);
    }

    /**
     * Creates a Version (Prepending the parent)
     *
     * @param parent Parent Version
     * @param type Version Type
     * @param ints Version Numbers (Will be separated with dots)
     */
    public Version(Version parent, VersionType type, int... ints) {
        if (Util.isNull(type)) throw new NullPointerException();
        this.parent = parent;
        this.type = type;
        String string = Integer.toString(ints[0]);
        int i = 0;
        if (ints.length != 1) {
            do {
                i++;
                string = string + "." + ints[i];
            } while ((i + 1) != ints.length);
        }
        this.string = string;
    }

    /*
     * The internal toString() method
     * new Version(new Version("1.0.0"), VersionType.PRE_ALPHA, "7") would return:
     * 5 1.0.0 0 7
     */
    private String toInternalString() {
        String str = type.id + ' ' + string;
        if (parent != null) str = parent.toInternalString()+' '+str;
        return str;
    }

    /**
     * The default toString() method<br>
     * <br>
     * <b>new Version(new Version("1.0.0"), VersionType.PRE_ALPHA, "7")</b> would return:<br>
     * <b>1.0.0/pa7</b>
     *
     * @return Version as a String
     */
	@Override
	public String toString() {
        String str = (parent == null)?"":parent.toString()+'/'+type.shortname;
        str += string;
        return str;
	}

    /**
     * The full toString() method<br>
     * <br>
     * <b>new Version(new Version("1.0.0"), VersionType.PRE_ALPHA, "7")</b> would return:<br>
     * <b>r1.0.0/pa7</b>
     *
     * @return Version as a String
     */
    public String toFullString() {
        String str = type.shortname + string;
        if (parent != null) str = parent.toFullString()+'/'+str;
        return str;
    }

    /**
     * The extended toString() method<br>
     * <br>
     * <b>new Version(new Version("1.0.0"), VersionType.PRE_ALPHA, "7")</b> would return:<br>
     * <b>1.0.0 pre-alpha 7</b>
     *
     * @return Version as a String
     */
    public String toExtendedString() {
        String str = (parent == null)?"":parent.toExtendedString()+' '+type.longname+' ';
        str += string;
        return str;
    }

    /**
     * The full extended toString() method<br>
     * <br>
     * <b>new Version(new Version("1.0.0"), VersionType.PRE_ALPHA, "7")</b> would return:<br>
     * <b>release 1.0.0 pre-alpha 7</b>
     *
     * @return Version as a String
     */
    public String toFullExtendedString() {
        String str = type.longname + ' ' + string;
        if (parent != null) str = parent.toFullExtendedString()+' '+str;
        return str;
    }

	@Override
    public boolean equals(Object object) {
        if (object instanceof Version) {
            return equals((Version) object);
        } else {
            return super.equals(object);
        }
    }

    /**
     * See if Versions are Equal
     *
     * @param version Version to Compare
     * @return
     */
    public boolean equals(Version version) {
        return compareTo(version) == 0;
    }
    
    /*
     * Returns 1 if Greater than
     * Returns 0 if Equal
     * Returns -1 if Less than 
     *//**
     *
     * Compare Versions
     *
     * @param version The version to compare to
     */
    public int compareTo(Version version) {
        String version1 = toInternalString();
        String version2 = version.toInternalString();

        VersionTokenizer tokenizer1 = new VersionTokenizer(version1);
        VersionTokenizer tokenizer2 = new VersionTokenizer(version2);

        int number1 = 0, number2 = 0;
        String suffix1 = "", suffix2 = "";

        while (tokenizer1.MoveNext()) {
            if (!tokenizer2.MoveNext()) {
                do {
                    number1 = tokenizer1.getNumber();
                    suffix1 = tokenizer1.getSuffix();
                    if (number1 != 0 || suffix1.length() != 0) {
                        // Version one is longer than number two, and non-zero
                        return 1;
                    }
                }
                while (tokenizer1.MoveNext());

                // Version one is longer than version two, but zero
                return 0;
            }

            number1 = tokenizer1.getNumber();
            suffix1 = tokenizer1.getSuffix();
            number2 = tokenizer2.getNumber();
            suffix2 = tokenizer2.getSuffix();

            if (number1 < number2) {
                // Number one is less than number two
                return -1;
            }
            if (number1 > number2) {
                // Number one is greater than number two
                return 1;
            }

            boolean empty1 = suffix1.length() == 0;
            boolean empty2 = suffix2.length() == 0;

            if (empty1 && empty2) continue; // No suffixes
            if (empty1) return 1; // First suffix is empty (1.2 > 1.2b)
            if (empty2) return -1; // Second suffix is empty (1.2a < 1.2)

            // Lexical comparison of suffixes
            int result = suffix1.compareTo(suffix2);
            if (result != 0) return result;

        }
        if (tokenizer2.MoveNext()) {
            do {
                number2 = tokenizer2.getNumber();
                suffix2 = tokenizer2.getSuffix();
                if (number2 != 0 || suffix2.length() != 0) {
                    // Version one is longer than version two, and non-zero
                    return -1;
                }
            }
            while (tokenizer2.MoveNext());

            // Version two is longer than version one, but zero
            return 0;
        }
        return 0;
    }

    /**
     * See if Versions are Equal
     *
     * @param ver1 Version to Compare
     * @param ver2 Version to Compare
     * @return
     */
    public static boolean equals(Version ver1, Version ver2) {
        return compare(ver1, ver2) == 0;
    }

    /*
     * Returns 1 if Greater than
     * Returns 0 if Equal
     * Returns -1 if Less than
     *//**
     * Compare Versions
     *
     * @param ver1 Version to Compare
     * @param ver2 Version to Compare
     */
    public static int compare(Version ver1, Version ver2) {
        String version1 = ver1.toInternalString();
        String version2 = ver2.toInternalString();

        VersionTokenizer tokenizer1 = new VersionTokenizer(version1);
        VersionTokenizer tokenizer2 = new VersionTokenizer(version2);

        int number1 = 0, number2 = 0;
        String suffix1 = "", suffix2 = "";

        while (tokenizer1.MoveNext()) {
            if (!tokenizer2.MoveNext()) {
                do {
                    number1 = tokenizer1.getNumber();
                    suffix1 = tokenizer1.getSuffix();
                    if (number1 != 0 || suffix1.length() != 0) {
                        // Version one is longer than number two, and non-zero
                        return 1;
                    }
                }
                while (tokenizer1.MoveNext());

                // Version one is longer than version two, but zero
                return 0;
            }

            number1 = tokenizer1.getNumber();
            suffix1 = tokenizer1.getSuffix();
            number2 = tokenizer2.getNumber();
            suffix2 = tokenizer2.getSuffix();

            if (number1 < number2) {
                // Number one is less than number two
                return -1;
            }
            if (number1 > number2) {
                // Number one is greater than number two
                return 1;
            }

            boolean empty1 = suffix1.length() == 0;
            boolean empty2 = suffix2.length() == 0;

            if (empty1 && empty2) continue; // No suffixes
            if (empty1) return 1; // First suffix is empty (1.2 > 1.2b)
            if (empty2) return -1; // Second suffix is empty (1.2a < 1.2)

            // Lexical comparison of suffixes
            int result = suffix1.compareTo(suffix2);
            if (result != 0) return result;

        }
        if (tokenizer2.MoveNext()) {
            do {
                number2 = tokenizer2.getNumber();
                suffix2 = tokenizer2.getSuffix();
                if (number2 != 0 || suffix2.length() != 0) {
                    // Version one is longer than version two, and non-zero
                    return -1;
                }
            }
            while (tokenizer2.MoveNext());

            // Version two is longer than version one, but zero
            return 0;
        }
        return 0;
    }
}