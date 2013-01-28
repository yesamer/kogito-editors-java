package org.jboss.errai.ioc.client.container.async;

import org.jboss.errai.ioc.client.container.CreationalContext;
import org.jboss.errai.ioc.client.container.DestructionCallback;
import org.jboss.errai.ioc.client.container.IOCResolutionException;
import org.jboss.errai.ioc.client.container.IOCSingletonBean;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Mike Brock
 */
public class AsyncBeanManagerImpl implements AsyncBeanManager {
  /**
   * A map of all named beans.
   */
  private final Map<String, List<AsyncBeanDef>> namedBeans
      = new HashMap<String, List<AsyncBeanDef>>();

  /**
   * A map of all beans managed by the bean mean manager, keyed by type.
   */
  private final Map<Class<?>, List<AsyncBeanDef>> beanMap
      = new HashMap<Class<?>, List<AsyncBeanDef>>();

  /**
   * A map which contains bean instances as keys, and their associated {@link org.jboss.errai.ioc.client.container.CreationalContext}s as values.
   */
  private final Map<Object, CreationalContext> creationalContextMap
      = new IdentityHashMap<Object, CreationalContext>();

  /**
   * A map which contains proxied instances as keys, and the underlying proxied bean instances as values.
   */
  private final Map<Object, Object> proxyLookupForManagedBeans
      = new IdentityHashMap<Object, Object>();

  /**
   * A collection which contains a list of all known concrete bean types being managed. eg. no interface or
   * abstract types will be present in this collection.
   */
  private final Set<String> concreteBeans
      = new HashSet<String>();

  public AsyncBeanManagerImpl() {
    // java.lang.Object is "special" in that it is treated like a concrete bean type for the purpose of
    // lookups. This modifies the lookup behavior to exclude other non-concrete types from qualified matching.
    concreteBeans.add("java.lang.Object");
  }

  private AsyncBeanDef<Object> _registerSingletonBean(final Class<Object> type,
                                                      final Class<?> beanType,
                                                      final AsyncBeanProvider<Object> callback,
                                                      final Object instance,
                                                      final Annotation[] qualifiers,
                                                      final String name,
                                                      final boolean concrete) {

    return registerBean(AsyncSingletonBean.newBean(this, type, beanType, qualifiers, name, concrete, callback, instance));
  }

  private AsyncBeanDef<Object> _registerDependentBean(final Class<Object> type,
                                                      final Class<?> beanType,
                                                      final AsyncBeanProvider<Object> callback,
                                                      final Annotation[] qualifiers,
                                                      final String name,
                                                      final boolean concrete) {

    return registerBean(AsyncDependentBean.newBean(this, type, beanType, qualifiers, name, concrete, callback));
  }

  private void registerSingletonBean(final Class<Object> type,
                                     final Class<?> beanType,
                                     final AsyncBeanProvider<Object> callback,
                                     final Object instance,
                                     final Annotation[] qualifiers,
                                     final String beanName,
                                     final boolean concrete) {


    _registerNamedBean(beanName, _registerSingletonBean(type, beanType, callback, instance, qualifiers, beanName, concrete));
  }

  private void registerDependentBean(final Class<Object> type,
                                     final Class<?> beanType,
                                     final AsyncBeanProvider<Object> callback,
                                     final Annotation[] qualifiers,
                                     final String beanName,
                                     final boolean concrete) {

    _registerNamedBean(beanName, _registerDependentBean(type, beanType, callback, qualifiers, beanName, concrete));
  }

  private void _registerNamedBean(final String name,
                                  final AsyncBeanDef beanDef) {
    if (name == null) return;

    if (!namedBeans.containsKey(name)) {
      namedBeans.put(name, new ArrayList<AsyncBeanDef>());
    }
    namedBeans.get(name).add(beanDef);
  }

  /**
   * Register a bean with the manager. This is usually called by the generated code to advertise the bean. Adding
   * beans at runtime will make beans available for lookup through the BeanManager, but will not in any way alter
   * the wiring scenario of auto-discovered beans at runtime.
   *
   * @param type
   *     the bean type
   * @param beanType
   *     the actual type of the bean
   * @param callback
   *     the creational callback used to construct the bean
   * @param instance
   *     the instance reference
   * @param qualifiers
   *     any qualifiers
   */
  @Override
  public void addBean(final Class<Object> type,
                      final Class<?> beanType,
                      final AsyncBeanProvider<Object> callback,
                      final Object instance,
                      final Annotation[] qualifiers) {

    addBean(type, beanType, callback, instance, qualifiers, null, beanType.equals(type));
  }


  /**
   * Register a bean with the manager with a name. This is usually called by the generated code to advertise the bean.
   * Adding beans at runtime will make beans available for lookup through the BeanManager, but will not in any way alter
   * the wiring scenario of auto-discovered beans at runtime.
   *
   * @param type
   *     the bean type
   * @param beanType
   *     the actual type of the bean
   * @param callback
   *     the creational callback used to construct the bean
   * @param instance
   *     the instance reference
   * @param qualifiers
   *     any qualifiers
   * @param name
   *     the name of the bean
   */
  @Override
  public void addBean(final Class<Object> type,
                      final Class<?> beanType,
                      final AsyncBeanProvider<Object> callback,
                      final Object instance,
                      final Annotation[] qualifiers,
                      final String name) {

    addBean(type, beanType, callback, instance, qualifiers, name, true);
  }


  /**
   * Register a bean with the manager with a name as well as specifying whether the bean should be treated a concrete
   * type. This is usually called by the generated code to advertise the bean. Adding beans at runtime will make beans
   * available for lookup through the BeanManager, but will not in any way alter the wiring scenario of auto-discovered
   * beans at runtime.
   *
   * @param type
   *     the bean type
   * @param beanType
   *     the actual type of the bean
   * @param callback
   *     the creational callback used to construct the bean
   * @param instance
   *     the instance reference
   * @param qualifiers
   *     any qualifiers
   * @param name
   *     the name of the bean
   * @param concreteType
   *     true if bean should be treated as concrete (ie. not an interface or abstract type).
   */
  @Override
  public void addBean(final Class<Object> type,
                      final Class<?> beanType,
                      final AsyncBeanProvider<Object> callback,
                      final Object instance,
                      final Annotation[] qualifiers,
                      final String name,
                      final boolean concreteType) {

    if (concreteType) {
      concreteBeans.add(type.getName());
    }

    if (instance != null) {
      registerSingletonBean(type, beanType, callback, instance, qualifiers, name, concreteType);
    }
    else {
      registerDependentBean(type, beanType, callback, qualifiers, name, concreteType);
    }
  }


  /**
   * Destroy a bean and all other beans associated with its creational context in the bean manager.
   *
   * @param ref
   *     the instance reference of the bean
   */
  @SuppressWarnings("unchecked")
  public void destroyBean(final Object ref) {
    final AsyncCreationalContext creationalContext =
        (AsyncCreationalContext) creationalContextMap.get(getActualBeanReference(ref));

    if (creationalContext == null) {
      return;
    }

    creationalContext.destroyContext();

    for (final Object inst : creationalContext.getAllCreatedBeanInstances()) {
      creationalContextMap.remove(inst);
      proxyLookupForManagedBeans.remove(inst);
      proxyLookupForManagedBeans.values().remove(inst);
    }
  }

  /**
   * Indicates whether the referenced object is currently a managed bean.
   *
   * @param ref
   *     the reference to the bean
   *
   * @return returns true if under management
   */
  public boolean isManaged(final Object ref) {
    return creationalContextMap.containsKey(getActualBeanReference(ref));
  }

  /**
   * Obtains an instance to the <em>actual</em> bean. If the specified reference is a proxy, this method will
   * return an un-proxied reference to the object.
   *
   * @param ref
   *     the proxied or unproxied reference
   *
   * @return returns the absolute reference to bean if the specified reference is a proxy. If the specified reference
   *         is not a proxy, the same instance passed to the method is returned.
   *
   * @see #isProxyReference(Object)
   */
  public Object getActualBeanReference(final Object ref) {
    if (isProxyReference(ref)) {
      return proxyLookupForManagedBeans.get(ref);
    }
    else {
      return ref;
    }
  }

  /**
   * Determines whether the referenced object is itself a proxy to a managed bean.
   *
   * @param ref
   *     the reference to check
   *
   * @return returns true if the specified reference is itself a proxy.
   *
   * @see #getActualBeanReference(Object)
   */
  public boolean isProxyReference(final Object ref) {
    return proxyLookupForManagedBeans.containsKey(ref);
  }

  /**
   * Associates the reference to a proxied bean to the underlying bean instance which it is proxying.
   *
   * @param proxyRef
   *     the reference to the proxy
   * @param realRef
   *     the reference to the bean being proxied.
   */
  public void addProxyReference(final Object proxyRef, final Object realRef) {
    proxyLookupForManagedBeans.put(proxyRef, realRef);
  }

  /**
   * Associates a bean instance with a creational context.
   *
   * @param ref
   *     the reference to the bean
   * @param creationalContext
   *     the {@link CreationalContext} instance to associate the bean instance with.
   */
  public void addBeanToContext(final Object ref, final CreationalContext creationalContext) {
    creationalContextMap.put(ref, creationalContext);
  }

  /**
   * Register a bean with the manager.
   *
   * @param bean
   *     an {@link IOCSingletonBean} reference
   */
  @Override
  public <T> AsyncBeanDef<T> registerBean(final AsyncBeanDef<T> bean) {
    if (!beanMap.containsKey(bean.getType())) {
      beanMap.put(bean.getType(), new ArrayList<AsyncBeanDef>());
    }

    beanMap.get(bean.getType()).add(bean);
    return bean;
  }

  /**
   * Looks up all beans with the specified bean name as specified by {@link javax.inject.Named}.
   *
   * @param name
   *     the name of bean to lookup
   *
   * @return and unmodifiable list of all beans with the specified name.
   */
  @Override
  public Collection<AsyncBeanDef> lookupBeans(final String name) {
    if (!namedBeans.containsKey(name)) {
      return Collections.emptyList();
    }
    else {
      return namedBeans.get(name);
    }
  }

  /**
   * Looks up all beans of the specified type.
   *
   * @param type
   *     The type of the bean
   *
   * @return An unmodifiable list of all the beans that match the specified type. Returns an empty list if there is
   *         no matching type.
   */
  @Override
  @SuppressWarnings("unchecked")
  public <T> Collection<AsyncBeanDef<T>> lookupBeans(final Class<T> type) {
    final List<AsyncBeanDef> beanList;

    if (type.getName().equals("java.lang.Object")) {
      beanList = new ArrayList<AsyncBeanDef>();
      for (final List<AsyncBeanDef> list : beanMap.values()) {
        beanList.addAll(list);
      }
    }
    else {
      beanList = beanMap.get(type);
    }

    final List<AsyncBeanDef<T>> matching = new ArrayList<AsyncBeanDef<T>>();

    if (beanList != null) {
      for (final AsyncBeanDef<T> beanDef : beanList) {
        matching.add(beanDef);
      }
    }

    return Collections.unmodifiableList(matching);
  }

  /**
   * Looks up a bean reference based on type and qualifiers. Returns <tt>null</tt> if there is no type associated
   * with the specified
   *
   * @param type
   *     The type of the bean
   * @param qualifiers
   *     qualifiers to match
   *
   * @return An unmodifiable list of all beans which match the specified type and qualifiers. Returns an empty list
   *         if no beans match.
   */
  @Override
  @SuppressWarnings("unchecked")
  public <T> Collection<AsyncBeanDef<T>> lookupBeans(final Class<T> type, final Annotation... qualifiers) {
    final List<AsyncBeanDef> beanList;

    if (type.getName().equals("java.lang.Object")) {
      beanList = new ArrayList<AsyncBeanDef>();
      for (final List<AsyncBeanDef> list : beanMap.values()) {
        beanList.addAll(list);
      }
    }
    else {
      beanList = beanMap.get(type);
    }

    if (beanList == null) {
      return Collections.emptyList();
    }
    else if (beanList.size() == 1) {
      return Collections.singletonList((AsyncBeanDef<T>) beanList.iterator().next());
    }

    final List<AsyncBeanDef<T>> matching = new ArrayList<AsyncBeanDef<T>>();

    final Set<Annotation> qualifierSet = new HashSet<Annotation>(qualifiers.length * 2);
    Collections.addAll(qualifierSet, qualifiers);

    for (final AsyncBeanDef iocBean : beanList) {
      if (iocBean.matches(qualifierSet)) {
        matching.add(iocBean);
      }
    }

    if (matching.size() == 1) {
      return Collections.unmodifiableList(matching);
    }

    if (matching.size() > 1) {
      // perform second pass
      final Iterator<AsyncBeanDef<T>> secondIterator = matching.iterator();

      if (concreteBeans.contains(type.getName())) {
        while (secondIterator.hasNext()) {
          if (!secondIterator.next().isConcrete())
            secondIterator.remove();
        }
      }
      else {
        while (secondIterator.hasNext()) {
          if (!concreteBeans.contains(secondIterator.next().getBeanClass().getName()))
            secondIterator.remove();
        }
      }
    }

    return Collections.unmodifiableList(matching);
  }

  /**
   * Looks up a bean reference based on type and qualifiers. Returns <tt>null</tt> if there is no type associated
   * with the specified
   *
   * @param type
   *     The type of the bean
   * @param qualifiers
   *     qualifiers to match
   * @param <T>
   *     The type of the bean
   *
   * @return An instance of the {@link IOCSingletonBean} for the matching type and qualifiers.
   *         Throws an {@link org.jboss.errai.ioc.client.container.IOCResolutionException} if there is a matching type but none of the
   *         qualifiers match or if more than one bean  matches.
   */
  @Override
  @SuppressWarnings("unchecked")
  public <T> AsyncBeanDef<T> lookupBean(final Class<T> type, final Annotation... qualifiers) {
    final Collection<AsyncBeanDef<T>> matching = lookupBeans(type, qualifiers);

    if (matching.size() == 1) {
      return matching.iterator().next();
    }
    else if (matching.isEmpty()) {
      throw new IOCResolutionException("no matching bean instances for: " + type.getName());
    }
    else {
      throw new IOCResolutionException("multiple matching bean instances for: " + type.getName() + " matches: " + matching);
    }
  }


  /**
   * Associates a {@link org.jboss.errai.ioc.client.container.DestructionCallback} with a bean instance. If the bean manager cannot find a valid
   * {@link CreationalContext} to associate with the bean, or the bean is no longer considered active, the method
   * returns <tt>false</tt>. Otherwise, the method returns <tt>true</tt>, indicating the callback is now registered
   * and will be called when the bean is destroyed.
   *
   * @param beanInstance
   *     the bean instance to associate the callback to.
   * @param destructionCallback
   *     the instance of the {@link org.jboss.errai.ioc.client.container.DestructionCallback}.
   *
   * @return <tt>true</tt> if the {@link org.jboss.errai.ioc.client.container.DestructionCallback} is successfully registered against a valid
   *         {@link CreationalContext} and <tt>false</tt> if not.
   */
  public boolean addDestructionCallback(final Object beanInstance, final DestructionCallback<?> destructionCallback) {
    final CreationalContext creationalContext = creationalContextMap.get(beanInstance);
    if (creationalContext == null) {
      return false;
    }

    creationalContext.addDestructionCallback(beanInstance, destructionCallback);
    return true;
  }

  public void destroyAllBeans() {
    namedBeans.clear();
    beanMap.clear();
  }
}
