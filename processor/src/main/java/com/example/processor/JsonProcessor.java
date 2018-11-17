package com.example.processor;

import com.example.annotation.InjectView;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

public class JsonProcessor extends AbstractProcessor {

    private static final String GEN_CLASS_SUFFIX = "Injector";
    private static final String INJECTOR_NAME = "ViewInjector";

    private Types mTypeUtils;

    private Elements mElementUtils;

    private Filer mFiler;

    private Messager mMessager;

    /**
     * 该方法是初始化的地方，我们可以通过ProcessingEnvironment 获取很多有用的工具类
     *
     * @param processingEnv
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        mTypeUtils = processingEnv.getTypeUtils();

        mElementUtils = processingEnv.getElementUtils();

        mFiler = processingEnv.getFiler();

        mMessager = processingEnv.getMessager();
    }

    /**
     * 这个方法指定处理的注解，需要将处理的注解的全名放到Set中，并返回
     * <p>
     * 可以被 @SupportedAnnotationTypes注解 替代
     *
     * @return
     */
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        HashSet<String> set = new HashSet<>();
        set.add(InjectView.class.getCanonicalName());
        return set;
    }

    /**
     * 这个方法 用来指定 支持的Java版本
     * <p>
     * 可以被 @SupportedSourceVersion 替代
     *
     * @return
     */
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    /**
     * 实际处理注解的方法
     *
     * @param annotations
     * @param roundEnv
     * @return
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(InjectView.class);

        //process会被调用三次，只有一次是可以处理InjectView注解的，原因不明
        if (elements.size() == 0) {
            return true;
        }

        Map<Element, List<Element>> elementListMap = new HashMap<>();

        StringBuffer buffer = new StringBuffer();
        buffer.append("package com.example.processor.injector;\n")
                .append("public class " + INJECTOR_NAME + " {\n");

        // 遍历所有被InjectView注释的元素
        for (Element element : elements) {
            //如果标注的对象不是FIELD则报错,这个错误其实不会发生因为InjectView的Target已经声明为ElementType.FIELD了
            if (element.getKind() != ElementKind.FIELD) {
                mMessager.printMessage(Diagnostic.Kind.ERROR, " is not a FIELD", element);
            }

            // 如果不是View的子类，则报错
            if (!isView(element.asType())) {
                mMessager.printMessage(Diagnostic.Kind.ERROR, " is not a View", element);
            }

            // 获取所在的类信息
            Element clazz = element.getEnclosingElement();

            // 按类存入 Map中
            addElement(elementListMap, clazz, element);
        }

        for (Element keyElement : elementListMap.keySet()) {
            Element clazz = keyElement;

            // 获取类名
            String className = clazz.getSimpleName().toString();

            // 获取所在的包名
            String packageName = mElementUtils.getPackageOf(clazz).asType().toString();

            // 生成注入代码
            generateInjectorCode(packageName, className, elementListMap.get(keyElement));

            // 完整类名
            String fullName = clazz.asType().toString();

            buffer.append("\tpublic static void inject(" + fullName + " arg) {\n")
                    .append("\t\t" + fullName + GEN_CLASS_SUFFIX + ".inject(arg);\n")
                    .append("\t}\n");
        }

        buffer.append("}");

        generateCode(INJECTOR_NAME, buffer.toString());

        return true;
    }

    /**
     * 递归 判断 android.view.View 是不是其父类
     *
     * @param type
     * @return
     */
    private boolean isView(TypeMirror type) {
        List<? extends TypeMirror> supers = mTypeUtils.directSupertypes(type);
        if (supers.size() == 0) {
            return false;
        }
        for (TypeMirror superType : supers) {
            if (superType.toString().equals("android.view.View") || isView(superType)) {
                return true;
            }
        }
        return false;
    }

    private void addElement(Map<Element, List<Element>> map, Element clazz, Element field) {
        List<Element> list = map.get(clazz);
        if (list == null) {
            list = new ArrayList<>();
            map.put(clazz, list);
        }
        list.add(field);
    }

    private void generateCode(String className, String code) {
        try {
            JavaFileObject file = mFiler.createSourceFile(className);
            Writer writer = file.openWriter();
            writer.write(code);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 生成注入代码
     *
     * @param packageName 包名
     * @param className   类名
     * @param views       需要注入的成员变量
     */
    private void generateInjectorCode(String packageName, String className, List<Element> views) {
        StringBuilder builder = new StringBuilder();
        builder.append("package " + packageName + ";\n\n")
                .append("public class " + className + GEN_CLASS_SUFFIX + " {\n")
                .append("\tpublic static void inject(" + className + " arg) {\n");

        for (Element element : views) {
            //获取变量类型
            String type = element.asType().toString();

            //获取变量名
            String name = element.getSimpleName().toString();

            //id
            int resourceId = element.getAnnotation(InjectView.class).value();

            builder.append("\t\targ." + name + "=(" + type + ")arg.findViewById(" + resourceId + ");\n");
        }

        builder.append("\t}\n")
                .append("}");

        //生成代码
        generateCode(className + GEN_CLASS_SUFFIX, builder.toString());
    }
}
