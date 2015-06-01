package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import compilation.CompilationAndRunResult;
import compilation.StringCompilerAndRunner;
import datas.keywords.KeywordsManager;
import datas.report.Report;
import datas.report.ReportManager;
import datas.samples.Sample;
import datas.samples.SampleManager;
import play.Logger;
import play.Play;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.index;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.Package;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.rmi.server.LoaderHandler;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.Collection;
import java.util.Map;

public class Application extends Controller {

    private static final String INTERFACE_CHOCO = "interface ChocoProject {" + "\n"
            + " void run();" + "\n"
            + "}" + "\n";

    public static Result index() {
        return ok(index.render("Let's get working guys !"));
    }

    /**
     * Call to compilation service
     *
     * @return result compilation result
     */
    public static Result compile() throws IllegalAccessException, InstantiationException {
        try {
            final Map<String, String[]> mapParameters = request().body().asFormUrlEncoded();
            String code = mapParameters.get("body")[0] + INTERFACE_CHOCO;
            System.out.println("Code re�u : " + code);
            ClassLoader cl = Play.application().classloader();
            //MyClassLoader tempCl = new MyClassLoader(cl);

            StringCompilerAndRunner compilerAndRunner = new StringCompilerAndRunner(cl);
            CompilationAndRunResult result = compilerAndRunner.compileAndRun(code);
            return ok(new ObjectMapper().<JsonNode>valueToTree(result));
        } catch(Exception e){
            Logger.warn("Problem while compiling and running", e);
            return internalServerError("Problem while compiling and running : "+e.getMessage());
        }
    }

    public static Result getCodeSampleList() {
        try {
            Collection<Sample> availableSample = SampleManager.getInstance().getAvailableSample();
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(availableSample);
            return ok(json);
        } catch (Exception e) {
            Logger.warn("Problem while getting the samples", e);
            return internalServerError("Problem while getting the samples : " + e.getMessage());
        }
    }

    public static Result reportError(String sourceCode, String userEmail, String stdOut, String stdErr, String compilationErr) {
        Report report;
        //Compilation error
        if (stdErr == null || stdErr.trim().isEmpty()) {
            report = ReportManager.getInstance().createReportCompilation(sourceCode, stdOut, compilationErr, userEmail);
        }
        //Execution error
        else {
            report = ReportManager.getInstance().createReportExecution(sourceCode, stdOut, stdErr, userEmail);
        }
        boolean sent = ReportManager.getInstance().sendReport(report);
        return ok(""+sent);
    }

    public static Result getKeywords() throws IOException, ClassNotFoundException {
        String chocoClasses = KeywordsManager.getChocoClassesName();
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(chocoClasses);
        return ok(json);
    }

}
