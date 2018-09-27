package com.firefly.utils.io;

import com.firefly.utils.Assert;
import com.firefly.utils.ClassUtils;
import com.firefly.utils.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Default implementation of the {@link ResourceLoader} interface.
 *
 * <p>Will return a {@link UrlResource} if the location value is a URL,
 * and a {@link ClassPathResource} if it is a non-URL path or a
 * "classpath:" pseudo-URL.
 */
public class DefaultResourceLoader implements ResourceLoader {

    private ClassLoader classLoader;

    private final Set<ProtocolResolver> protocolResolvers = new LinkedHashSet<ProtocolResolver>(4);


    /**
     * Create a new DefaultResourceLoader.
     * <p>ClassLoader access will happen using the thread context class loader
     * at the time of this ResourceLoader's initialization.
     *
     * @see java.lang.Thread#getContextClassLoader()
     */
    public DefaultResourceLoader() {
        this.classLoader = ClassUtils.getDefaultClassLoader();
    }

    /**
     * Create a new DefaultResourceLoader.
     *
     * @param classLoader the ClassLoader to load class path resources with, or {@code null}
     *                    for using the thread context class loader at the time of actual resource access
     */
    public DefaultResourceLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }


    /**
     * Specify the ClassLoader to load class path resources with, or {@code null}
     * for using the thread context class loader at the time of actual resource access.
     * <p>The default is that ClassLoader access will happen using the thread context
     * class loader at the time of this ResourceLoader's initialization.
     */
    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Return the ClassLoader to load class path resources with.
     * <p>Will get passed to ClassPathResource's constructor for all
     * ClassPathResource objects created by this resource loader.
     *
     * @see ClassPathResource
     */
    @Override
    public ClassLoader getClassLoader() {
        return (this.classLoader != null ? this.classLoader : ClassUtils.getDefaultClassLoader());
    }

    /**
     * Register the given resolver with this resource loader, allowing for
     * additional protocols to be handled.
     * <p>Any such resolver will be invoked ahead of this loader's standard
     * resolution rules. It may therefore also override any default rules.
     *
     * @see #getProtocolResolvers()
     * @since 4.3
     */
    public void addProtocolResolver(ProtocolResolver resolver) {
        Assert.notNull(resolver, "ProtocolResolver must not be null");
        this.protocolResolvers.add(resolver);
    }

    /**
     * Return the collection of currently registered protocol resolvers,
     * allowing for introspection as well as modification.
     *
     * @since 4.3
     */
    public Collection<ProtocolResolver> getProtocolResolvers() {
        return this.protocolResolvers;
    }


    @Override
    public Resource getResource(String location) {
        Assert.notNull(location, "Location must not be null");

        for (ProtocolResolver protocolResolver : this.protocolResolvers) {
            Resource resource = protocolResolver.resolve(location, this);
            if (resource != null) {
                return resource;
            }
        }

        if (location.startsWith("/")) {
            return getResourceByPath(location);
        } else if (location.startsWith(CLASSPATH_URL_PREFIX)) {
            return new ClassPathResource(location.substring(CLASSPATH_URL_PREFIX.length()), getClassLoader());
        } else {
            try {
                // Try to parse the location as a URL...
                URL url = new URL(location);
                return new UrlResource(url);
            } catch (MalformedURLException ex) {
                // No URL -> resolve as resource path.
                return getResourceByPath(location);
            }
        }
    }

    /**
     * Return a Resource handle for the resource at the given path.
     * <p>The default implementation supports class path locations. This should
     * be appropriate for standalone implementations but can be overridden,
     * e.g. for implementations targeted at a Servlet container.
     *
     * @param path the path to the resource
     * @return the corresponding Resource handle
     * @see ClassPathResource
     */
    protected Resource getResourceByPath(String path) {
        return new ClassPathContextResource(path, getClassLoader());
    }


    /**
     * ClassPathResource that explicitly expresses a context-relative path
     * through implementing the ContextResource interface.
     */
    protected static class ClassPathContextResource extends ClassPathResource implements ContextResource {

        public ClassPathContextResource(String path, ClassLoader classLoader) {
            super(path, classLoader);
        }

        @Override
        public String getPathWithinContext() {
            return getPath();
        }

        @Override
        public Resource createRelative(String relativePath) {
            String pathToUse = StringUtils.applyRelativePath(getPath(), relativePath);
            return new ClassPathContextResource(pathToUse, getClassLoader());
        }
    }

}
