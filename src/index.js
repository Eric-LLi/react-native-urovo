// main index.js

import { NativeModules, NativeEventEmitter } from 'react-native';

const { Urovo } = NativeModules;

const events = {};

const eventEmitter = new NativeEventEmitter(Urovo);

Urovo.on = (event, handler) => {
	const eventListener = eventEmitter.addListener(event, handler);

	events[event] =  events[event] ? [...events[event], eventListener]: [eventListener];
};

Urovo.off = (event) => {
	if (Object.hasOwnProperty.call(events, event)) {
		const eventListener = events[event].shift();

		if(eventListener) eventListener.remove();
	}
};

Urovo.removeAll = (event) => {
	if (Object.hasOwnProperty.call(events, event)) {
		eventEmitter.removeAllListeners(event);

		events[event] = [];
	}
}

export default Urovo;
