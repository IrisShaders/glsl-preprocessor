/*
 * Anarres C Preprocessor
 * Copyright (c) 2007-2015, Shevek
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * Modified by the contributors of glsl-preprocessor.
 */
package io.github.douira.glsl_preprocessor;

class State {

	boolean parent;
	boolean active;
	boolean sawElse;

	State() {
		this.parent = true;
		this.active = true;
		this.sawElse = false;
	}

	State(State parent) {
		this.parent = parent.isParentActive() && parent.isActive();
		this.active = true;
		this.sawElse = false;
	}

	/* Required for #elif */
	void setParentActive(boolean b) {
		this.parent = b;
	}

	boolean isParentActive() {
		return parent;
	}

	void setActive(boolean b) {
		this.active = b;
	}

	boolean isActive() {
		return active;
	}

	void setSawElse() {
		sawElse = true;
	}

	boolean sawElse() {
		return sawElse;
	}

	@Override
	public String toString() {
		return "parent=" + parent
				+ ", active=" + active
				+ ", sawelse=" + sawElse;
	}
}
