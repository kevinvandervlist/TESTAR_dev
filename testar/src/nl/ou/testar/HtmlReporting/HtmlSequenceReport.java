package nl.ou.testar.HtmlReporting;

import nl.ou.testar.a11y.reporting.HTMLReporter;
import org.fruit.alayer.Action;
import org.fruit.alayer.State;
import org.fruit.alayer.Tags;
import org.fruit.alayer.Widget;

import java.io.File;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;

public class HtmlSequenceReport {

    private int sequenceCounter = 0;

    private static final String[] HEADER = new String[] {
            "<!DOCTYPE html>",
            "<html>",
            "<head>",
            "<title>TESTAR execution sequence report</title>",
            "</head>",
            "<body>"
    };

    private PrintWriter out;
    private static final String REPORT_FILENAME_PRE = "TESTAR_sequence_";
    private static final String REPORT_FILENAME_AFT = ".html";

    public HtmlSequenceReport() {
        try{
            //TODO put filename into settings, name with sequence number
            // creating a new file for the report:
            String filename = "TESTAR_sequence.txt"; // will be replaced
            int i = 1;
            boolean newFilenameFound = false;
            while(!newFilenameFound){
                filename = REPORT_FILENAME_PRE+i+REPORT_FILENAME_AFT;
                File file = new File(filename);
                if(file.exists()){
                    i++;
                }else{
                    newFilenameFound = true;
                }
            }
            out = new PrintWriter(filename, HTMLReporter.CHARSET);
            for(String s:HEADER){
                write(s);
            }
            write("<h1>TESTAR execution sequence report</h1>");
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void addState(State state, Set<Action> actions){
        write("<h2>State "+sequenceCounter+"</h2>");
        write("<h4>concreteID="+state.get(Tags.ConcreteID)+"</h4>");
        try{if(state.get(Tags.AbstractID)!=null) write("<h4>abstractID="+state.get(Tags.AbstractID)+"</h4>");}catch(Exception e){}
        write("<p><img src=\""+state.get(Tags.ScreenshotPath)+"\"></p>"); //<img src="smiley.gif" alt="Smiley face" height="42" width="42">
        sequenceCounter++;

        write("<h4>Set of widgets:</h4><ul>");
        for(Widget widget:state) {
            write("<li>");
            try{if(widget.get(Tags.Desc)!=null) write("<b>"+widget.get(Tags.Desc)+"</b> :: ");}catch(Exception e){}
            try{if(widget.get(Tags.Title)!=null) write(widget.get(Tags.Title)+" :: ");}catch(Exception e){}
            try{if(widget.get(Tags.Shape)!=null) write("X="+widget.get(Tags.Shape).x()+",Y="+widget.get(Tags.Shape).y()+", Width="+widget.get(Tags.Shape).width()+", Height="+widget.get(Tags.Shape).height());}catch(Exception e){}
            write("</li>");
        }
        write("</ul>");

        write("<h4>Set of actions:</h4><ul>");
        for(Action action:actions){
            write("<li>");
//            try{if(action.get(Tags.Role)!=null) write("--Role="+action.get(Tags.Role));}catch(Exception e){}
//            try{if(action.get(Tags.Targets)!=null) write("--Targets="+action.get(Tags.Targets));}catch(Exception e){}
            try{if(action.get(Tags.Desc)!=null) write("<b>"+action.get(Tags.Desc)+"</b>  || ");}catch(Exception e){}
            write(action.toString());
            write(" || ConcreteId="+action.get(Tags.ConcreteID));
            try{if(action.get(Tags.AbstractID)!=null) write(" || AbstractId="+action.get(Tags.AbstractID));}catch(Exception e){}
            write("</li>");
        }
        write("</ul>");
    }

    public void addState(State state, Set<Action> actions, Set<String> concreteIdsOfUnvisitedActions){
        write("<h2>State "+sequenceCounter+"</h2>");
        write("<h4>concreteID="+state.get(Tags.ConcreteID)+"</h4>");
        write("<p><img src=\""+state.get(Tags.ScreenshotPath)+"\"></p>"); //<img src="smiley.gif" alt="Smiley face" height="42" width="42">
        sequenceCounter++;
        if(actions.size()==concreteIdsOfUnvisitedActions.size()){
            write("<h4>Set of actions (all unvisited - a new state):</h4><ul>");
            for(Action action:actions){
                write("<li>");
                try{if(action.get(Tags.Desc)!=null) write("<b>"+action.get(Tags.Desc)+"</b>");}catch(Exception e){}
                write(" || ConcreteID="+action.get(Tags.ConcreteID)+" || "+action.toString());
                write("</li>");
            }
            write("</ul>");
        }else if(concreteIdsOfUnvisitedActions.size()==0){
            write("<h4>All actions have been visited, set of available actions:</h4><ul>");
            for(Action action:actions){
                write("<li>");
                try{if(action.get(Tags.Desc)!=null) write("<b>"+action.get(Tags.Desc)+"</b>");}catch(Exception e){}
                write(" || ConcreteID="+action.get(Tags.ConcreteID)+" || "+action.toString());
                write("</li>");
            }
            write("</ul>");
        }else{
            write("<h4>"+concreteIdsOfUnvisitedActions.size()+" out of "+actions.size()+" actions have not been visited yet:</h4><ul>");
            for(Action action:actions){
                if(concreteIdsOfUnvisitedActions.contains(action.get(Tags.ConcreteID))){
                    //action is unvisited -> showing:
                    write("<li>");
                    try{if(action.get(Tags.Desc)!=null) write("<b>"+action.get(Tags.Desc)+"</b>");}catch(Exception e){}
                    write(" || ConcreteID="+action.get(Tags.ConcreteID)+" || "+action.toString());
                    write("</li>");
                }
            }
            write("</ul>");
        }
    }

    public void addSelectedAction(String state_path, Action action){
//        System.out.println("path="+state_path);
        String actionPath = state_path.substring(0,state_path.indexOf(".png"));
//        System.out.println("path="+actionPath);
        actionPath = actionPath+"_"+action.get(Tags.ConcreteID)+".png";
//        System.out.println("path="+actionPath);
        write("<h2>Selected Action "+sequenceCounter+" leading to State "+sequenceCounter+"\"</h2>");
        write("<h4>concreteID="+action.get(Tags.ConcreteID));
        try{if(action.get(Tags.Desc)!=null) write(" || "+action.get(Tags.Desc));}catch(Exception e){}
        write("</h4>");
        write("<p><img src=\""+actionPath+"\"></p>"); //<img src="smiley.gif" alt="Smiley face" height="42" width="42">
    }
    
    public void close() {
        for(String s:HTMLReporter.FOOTER){
            write(s);
        }
        out.close();
    }

    private void write(String s) {
        out.println(s);
        out.flush();
    }

    private String start(String tag) {
        return "<" + tag + ">";
    }

    private String end(String tag) {
        return "</" + tag + ">";
    }
}
