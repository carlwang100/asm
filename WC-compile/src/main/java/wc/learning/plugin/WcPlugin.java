package wc.learning.plugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class WcPlugin implements Plugin<Project> {

    @Override
    public void apply(Project o) {
        System.out.println("i am best--------->>>>>>");
    }
}
