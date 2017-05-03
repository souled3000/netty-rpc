package com.blackcrystalinfo.platform.powersocket.log;

import org.springframework.stereotype.Repository;

@Repository
public class NullLogger implements ILogger {

	@Override
	public void write(String log) {

	}

}
