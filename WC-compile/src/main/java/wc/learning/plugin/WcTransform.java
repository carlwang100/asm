package wc.learning.plugin;

import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.Format;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformException;
import com.android.build.api.transform.TransformInput;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.api.transform.TransformOutputProvider;
import com.android.build.gradle.internal.pipeline.TransformManager;

import org.apache.commons.io.FileUtils;
import org.gradle.api.Project;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

public class WcTransform extends Transform {

    Project project;

    WcTransform(Project project) {
        this.project = project;
    }

    @Override
    public void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation);
        System.out.println("===== ASM Transform =====");
        System.out.println("${transformInvocation.inputs}");
        System.out.println("${transformInvocation.referencedInputs}");
        System.out.println("${transformInvocation.outputProvider}");
        System.out.println("${transformInvocation.incremental}");
        //当前是否是增量编译
        boolean isIncremental = transformInvocation.isIncremental();
        //消费型输入，可以从中获取jar包和class文件夹路径。需要输出给下一个任务
        Collection<TransformInput> inputs = transformInvocation.getInputs();
        //引用型输入，无需输出。
        Collection<TransformInput> referencedInputs = transformInvocation.getReferencedInputs();
        //OutputProvider管理输出路径，如果消费型输入为空，你会发现OutputProvider == null
        TransformOutputProvider outputProvider = transformInvocation.getOutputProvider();
        for (TransformInput input : inputs) {
            for (JarInput jarInput : input.getJarInputs()) {
                File dest = outputProvider.getContentLocation(
                        jarInput.getFile().getAbsolutePath(),
                        jarInput.getContentTypes(),
                        jarInput.getScopes(),
                        Format.JAR);
                //将修改过的字节码copy到dest，就可以实现编译期间干预字节码的目的了
                transformJar(jarInput.getFile(), dest);
            }
            for (DirectoryInput directoryInput : input.getDirectoryInputs()) {
                System.out.println("== DI = " + directoryInput.getFile().listFiles());
                File dest = outputProvider.getContentLocation(directoryInput.getName(),
                        directoryInput.getContentTypes(), directoryInput.getScopes(),
                        Format.DIRECTORY);
                //将修改过的字节码copy到dest，就可以实现编译期间干预字节码的目的了
                //FileUtils.copyDirectory(directoryInput.getFile(), dest)
                transformDir(directoryInput.getFile(), dest);
            }
        }
    }


    @Override
    public String getName() {
        return "wangc";
    }

    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS;
    }

    @Override
    public Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT;
    }


    @Override
    public boolean isIncremental() {
        return false;
    }


    private static void transformJar(File input, File dest) throws IOException {
        System.out.println("=== transformJar ===");
        System.out.println("=== transform dir = " + input.getAbsolutePath() + ", " + dest.getAbsolutePath());
        FileUtils.copyFile(input, dest);
    }

    private static void transformDir(File input, File dest) throws IOException {
        if (dest.exists()) {
            FileUtils.forceDelete(dest);
        }
        FileUtils.forceMkdir(dest);
        String srcDirPath = input.getAbsolutePath();
        String destDirPath = dest.getAbsolutePath();
        System.out.println("=== transform dir = " + srcDirPath + ", " + destDirPath);
        for (File file : input.listFiles()) {
            String destFilePath = file.getAbsolutePath().replace(srcDirPath, destDirPath);
            File destFile = new File(destFilePath);
            if (file.isDirectory()) {
                transformDir(file, destFile);
            } else if (file.isFile()) {
                FileUtils.touch(destFile);
                transformSingleFile(file, destFile);
            }
        }
    }

    private static void transformSingleFile(File input, File dest) {
        System.out.println("=== transformSingleFile ===");
        weave(input.getAbsolutePath(), dest.getAbsolutePath());
    }

    private static void weave(String inputPath, String outputPath) {
        System.out.println("=== weave = " + inputPath + ", " + outputPath);
        try {
            FileInputStream is = new FileInputStream(inputPath);
            ClassReader cr = new ClassReader(is);
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            TestMethodClassAdapter adapter = new TestMethodClassAdapter(cw);
            cr.accept(adapter, 0);
            FileOutputStream fos = new FileOutputStream(outputPath);
            fos.write(cw.toByteArray());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
