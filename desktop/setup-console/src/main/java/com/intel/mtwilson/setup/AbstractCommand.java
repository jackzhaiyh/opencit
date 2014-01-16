/*
 * Copyright (C) 2012 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.setup;

import com.intel.dcsg.cpg.validation.ObjectModel;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Subclasses must implement validate() from ObjectModel, note any faults
 * such as missing or invalid options.
 * Subclasses must implement execute() from Command.
 * Any errors encountered inside execute() should be thrown as exceptions.
 * @author jbuhacoff
 */
public abstract class AbstractCommand extends ObjectModel implements Command {
    protected Logger log = LoggerFactory.getLogger(getClass());
    protected SetupContext ctx = null;
    protected Configuration options = null;

    @Override
    public void setContext(SetupContext ctx) {
        this.ctx = ctx;
    }
    
    @Override
    public void setOptions(Configuration options) {
        this.options = options;
    }
    
    /**
     * 
     * @param args list of option names that are required
     */
    protected void requireOptions(String... args) {
        for(String arg : args) {
            String value = options.getString(arg);
            if( value == null ) {
                fault("required option %s is missing", arg);
                continue;
            }
            if( value.isEmpty() ) {
                fault("required option %s is empty", arg);
                continue;
            }
        }
    }
    
}
