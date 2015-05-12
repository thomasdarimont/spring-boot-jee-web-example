package demo;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebListener;
import javax.servlet.annotation.WebServlet;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.boot.context.embedded.ServletListenerRegistrationBean;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

class JeeComponentsBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

	private static final String WEB_SERVLET_ANNOTATION_TYPE = "javax.servlet.annotation.WebServlet";
	private static final String WEB_FILTER_ANNOTATION_TYPE = "javax.servlet.annotation.WebFilter";
	private static final String WEB_LISTENER_ANNOTATION_TYPE = "javax.servlet.annotation.WebListener";

	private static final Set<String> SUPPORTED_WEB_ANNOTATION_TYPES = new LinkedHashSet<>(Arrays.asList(
			WEB_SERVLET_ANNOTATION_TYPE, WEB_FILTER_ANNOTATION_TYPE, WEB_LISTENER_ANNOTATION_TYPE));

	private final String basePackage;

	public JeeComponentsBeanFactoryPostProcessor(String basePackage) {
		this.basePackage = basePackage;
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
		ClassLoader cl = ClassPathScanningCandidateComponentProvider.class.getClassLoader();

		configureIncludeFilters(provider, cl);

		Set<BeanDefinition> candiates = provider.findCandidateComponents(basePackage);

		registerJeeComponents(beanFactory, cl, candiates);
	}

	@SuppressWarnings("unchecked")
	private void configureIncludeFilters(ClassPathScanningCandidateComponentProvider provider, ClassLoader cl) {

		for (String annotationType : SUPPORTED_WEB_ANNOTATION_TYPES) {
			try {
				AnnotationTypeFilter filter = new AnnotationTypeFilter((Class<? extends Annotation>) ClassUtils.forName(
						annotationType, cl));
				provider.addIncludeFilter(filter);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	private void registerJeeComponents(ConfigurableListableBeanFactory beanFactory, ClassLoader cl,
			Set<BeanDefinition> candiates) throws LinkageError {
		for (BeanDefinition bd : candiates) {

			try {

				Class<?> beanClass = ClassUtils.forName(bd.getBeanClassName(), cl);
				WebServlet webServlet = beanClass.getDeclaredAnnotation(WebServlet.class);
				WebFilter webFilter = beanClass.getDeclaredAnnotation(WebFilter.class);
				WebListener webListener = beanClass.getDeclaredAnnotation(WebListener.class);

				if (webServlet != null) {
					createAndRegisterServletBean((DefaultListableBeanFactory) beanFactory, bd, beanClass, webServlet);
				} else if (webFilter != null) {
					createAndRegisterServletFilterBean((DefaultListableBeanFactory) beanFactory, bd, beanClass, webFilter);
				} else if (webListener != null) {
					createAndRegisterWebListenerBean((DefaultListableBeanFactory) beanFactory, bd, beanClass, webListener);
				}

			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	private void createAndRegisterWebListenerBean(DefaultListableBeanFactory beanFactory, BeanDefinition bd,
			Class<?> beanClass, WebListener webListener) {

		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(ServletListenerRegistrationBean.class);
		builder.addPropertyValue("listener", bd);

		String beanName = StringUtils.uncapitalize(beanClass.getSimpleName());

		beanFactory.registerBeanDefinition(beanName, builder.getBeanDefinition());
	}

	private void createAndRegisterServletFilterBean(DefaultListableBeanFactory beanFactory, BeanDefinition bd,
			Class<?> beanClass, WebFilter webFilter) {

		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(FilterRegistrationBean.class);
		builder.addPropertyValue("filter", bd);
		builder.addPropertyValue("urlPatterns", new LinkedHashSet<>(Arrays.asList(webFilter.urlPatterns())));

		String beanName = webFilter.filterName().isEmpty() ? StringUtils.uncapitalize(beanClass.getSimpleName())
				: webFilter.filterName();

		beanFactory.registerBeanDefinition(beanName, builder.getBeanDefinition());
	}

	private void createAndRegisterServletBean(DefaultListableBeanFactory defaultListeableBeanFactory, BeanDefinition bd,
			Class<?> beanClass, WebServlet webServlet) {

		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(ServletRegistrationBean.class);
		builder.addPropertyValue("servlet", bd);
		builder.addPropertyValue("urlMappings", new LinkedHashSet<>(Arrays.asList(webServlet.urlPatterns())));

		String beanName = webServlet.name().isEmpty() ? StringUtils.uncapitalize(beanClass.getSimpleName()) : webServlet
				.name();
		defaultListeableBeanFactory.registerBeanDefinition(beanName, builder.getBeanDefinition());
	}
}
