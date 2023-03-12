package testdata;

public class GenericThrows {
    public static <E extends Throwable> void throwAsE(Throwable t) throws E {
        throw (E) t;
    }
}
