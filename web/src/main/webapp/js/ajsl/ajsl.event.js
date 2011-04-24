/*
 * Copyright (C) 2010 Julien SMADJA <julien dot smadja at gmail dot com> - Arnaud LEMAIRE <alemaire at norad dot fr>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

ajsl.event = {

	registerAll : function(eventObjs) {
		for (var evObj in eventObjs) {
			this.register(eventObjs[evObj]);
		}
	},

	register : function(eventObj) {
		for (var ev in eventObj) {
			
			var context = null;
			if (typeof eventObj.context == 'function') {
				context = eventObj.context();
			} else if (eventObj.context) {
				context = $(eventObj.context);
			}
			
			if (typeof eventObj[ev] == 'function') {
				
				var ppos = ev.indexOf('|');
				var selector = ev.substring(0, ppos);
				var event = ev.substring(ppos + 1).trim();
				if (!selector) {
					var elements = context;
				} else {
					var elements = $(selector, context);
				}
				
				if (elements.length == 0 && event != 'init') {
					LOG.error('nothing found to register event : ', ev);
					return;
				}
				
				if (event == 'init') {
					LOG.debug('init "', ev, '" event to', elements);
					eventObj[ev](elements);
				} else {
					LOG.debug('bind "', ev, '" event to', elements);					
					elements.bind(event, {eventObj : eventObj}, eventObj[ev]);
				}
				
			}
		}
	}
	
};