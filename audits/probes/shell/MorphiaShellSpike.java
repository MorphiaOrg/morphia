import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.mozilla.javascript.*;

public class MorphiaShellSpike {
    public static void main(String[] args) throws Exception {
        var reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        String row;
        while ((row = reader.readLine()) != null) {
            var fields = row.split("\t", -1);
            var raw = new String(Base64.getDecoder().decode(fields[1]), StandardCharsets.UTF_8);
            for (var mode : new String[]{"script", "expression"}) {
                var source = mode.equals("expression") ? "(" + raw + "\n)" : raw;
                String error = "";
                boolean success = false;
                try {
                    var env = new CompilerEnvirons();
                    env.setLanguageVersion(Context.VERSION_ECMASCRIPT);
                    env.setXmlAvailable(false);
                    env.setIdeMode(false);
                    env.setRecoverFromErrors(false);
                    env.setErrorReporter(new ErrorReporter() {
                        public void warning(String m,String s,int l,String a,int c) {}
                        public void error(String m,String s,int l,String a,int c) {throw runtimeError(m,s,l,a,c);}
                        public EvaluatorException runtimeError(String m,String s,int l,String a,int c) {
                            return new EvaluatorException(m,s,l,a,c);
                        }
                    });
                    new Parser(env).parse(source, fields[0], 1);
                    success = true;
                } catch (EvaluatorException failure) {
                    error = failure.getMessage();
                }
                System.out.println(fields[0]+"\t"+mode+"\t"+success+"\t"+
                    Base64.getEncoder().encodeToString(error.getBytes(StandardCharsets.UTF_8)));
            }
        }
    }
}
