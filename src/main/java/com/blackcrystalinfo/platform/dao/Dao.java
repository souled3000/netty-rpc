package com.blackcrystalinfo.platform.dao;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public interface Dao<T> {

	Serializable save(T entity);

	void delete(T entity);

	void update(T entity);

	T get(Serializable id);

	List<T> get(Collection<? extends Serializable> ids);

	List<T> getAll();

	boolean exists(Serializable id);

	<P> List<P> find(String query, Object[] params);

	<P> List<P> find(String query, Object[] params, int start, int count);

}
