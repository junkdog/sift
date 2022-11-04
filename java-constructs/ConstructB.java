package testdata;

import java.util.List;

public class ConstructB {
    public void foo() {
        fn(input -> new Payload());
    }

    void fn(Fn f) {}

    static class Payload {}
}
