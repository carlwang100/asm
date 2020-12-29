package wc.learning.plugin;

import com.android.build.gradle.AppExtension;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class WcPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        System.out.println("i am 1.0.3---------");
        AppExtension android = project.getExtensions().getByType(AppExtension.class);
        android.registerTransform(new WcTransform(project));
    }
}
