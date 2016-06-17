/**
 * Copyright 2014 Bill McDowell 
 *
 * This file is part of ARKWater (https://github.com/forkunited/ARKWater)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy 
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT 
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the 
 * License for the specific language governing permissions and limitations 
 * under the License.
 */

package edu.cmu.ml.rtw.generic.util;

/**
 * Pair holds a pair of objects
 * 
 * @author Bill McDowell
 *
 * @param <F> type of first object in pair
 * @param <S> type of second object in pair
 */
public class Pair<F, S> {
    private F first; 
    private S second; 

    public Pair(F first, S second) {
        this.first = first;
        this.second = second;
    }

    public boolean setFirst(F first) {
    	this.first = first;
    	return true;
    }
    
    public boolean setSecond(S second) {
    	this.second = second;
    	return true;
    }
    
    public F getFirst() {
        return this.first;
    }

    public S getSecond() {
        return this.second;
    }
    
    @SuppressWarnings("unchecked")
	@Override
    public boolean equals(Object o) {
    	Pair<F, S> p = (Pair<F, S>)o;
    	
    	if (p.getFirst() == null || this.getFirst() == null) {
	    	if ((p.getFirst() == null && this.getFirst() != null)
	    			|| (p.getFirst() != null && this.getFirst() == null))
	    		return false;
    	} else if (!p.getFirst().equals(this.getFirst())) {
    		return false;
    	}
    	
    	if (p.getSecond() == null || this.getSecond() == null) {
	    	if ((p.getSecond() == null && this.getSecond() != null)
	    			|| (p.getSecond() != null && this.getSecond() == null))
	    		return false;
    	} else if (!p.getSecond().equals(this.getSecond())) {
    		return false;
    	}
    
    	return true;
    }
    
    @Override
    public int hashCode() {
    	int h = 0;
    	if (this.first != null)
    		h ^= this.first.hashCode();
    	if (this.second != null)
    		h ^= this.second.hashCode(); 
    	return h;
    }
    
    @Override
    public String toString() {
    	return this.first.toString() + ", " + this.second.toString();
    }
}