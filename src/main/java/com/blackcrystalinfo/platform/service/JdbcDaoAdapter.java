package com.blackcrystalinfo.platform.service;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.stereotype.Repository;
@Repository
public class JdbcDaoAdapter extends JdbcDaoSupport {
	public Map<String,Object> queryForMap(String sql,Object... args){
		try{
			return this.queryForMap(sql, args);
		}catch (Exception e){
		}
		return null;
	}
	
	public Object QueryForObject(String sql,Class<?> clz,Object...args){
		try{
			return this.QueryForObject(sql, clz, args);
		}catch(Exception e ){
		}
		return null;
	}
	public Object QueryForLong(String sql,Class<?> clz,Object...args){
		try{
			return this.QueryForLong(sql, clz, args);
		}catch(Exception e ){
		}
		return 0L;
	}
	public Object QueryForInt(String sql,Class<?> clz,Object...args){
		try{
			return this.QueryForInt(sql, clz, args);
		}catch(Exception e ){
		}
		return 0;
	}
	public Object QueryForString(String sql,Class<?> clz,Object...args){
		try{
			Map<String,Object> v = this.queryForMap(sql, args);
			return (String)v.get(v.keySet().iterator().next());
		}catch(Exception e ){
		}
		return "";
	}
	
	public List<?> queryForList(String sql, Object... args){
		try{
			return this.queryForList(sql,args);
		}catch(Exception e ){
		}
		return null;
	}
	
	public List<?> queryForList(String sql,Class<?> clz,Object...args){
		try{
			return this.queryForList(sql,clz,args);
		}catch(Exception e ){
		}
		return null;
	}
	
	
}
