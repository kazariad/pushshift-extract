package dev.dkaz.pushshift.extract.filter;

import dev.dkaz.pushshift.extract.Args;
import dev.dkaz.pushshift.extract.Main;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

public class PythonFilter implements FilterStrategy {
    public static final FilterFactory FILTER_FACTORY = () -> {
        PythonFilter filter = new PythonFilter();
        filter.context = Context.newBuilder("python").engine(Main.ENGINE).allowAllAccess(true).build();
        filter.bindings = filter.context.getBindings("python");
        return filter;
    };

    private Context context;
    private Value bindings;

    @Override
    public boolean isAllowed(String line) {
        bindings.putMember("line", line);
        bindings.putMember("allow", Boolean.FALSE);
        context.eval(Args.pyScript);
        return bindings.getMember("allow").asBoolean();
    }

    @Override
    public void close() {
        if (context != null) context.close();
    }
}
