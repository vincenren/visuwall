
(function($) {

	$.fn.slideFadeToggle = function(speed, easing, callback) {
		return this.animate({
			opacity : 'toggle',
			height : 'toggle'
		}, speed, easing, callback);
	};

	$.fn.switchClasses = function(classSwitch, newClass, duration) {
		var obj = this;
		var oldClass;
		for ( var i = 0; i < classSwitch.length; i++) {
			if (obj.hasClass(classSwitch[i])) {
				oldClass = classSwitch[i];
				break;
			}
		}
		if (oldClass != newClass) {
			obj.switchClass(oldClass, newClass, duration);
		}
	};

	$.fn.stopBlink = function(options) {
		this.removeClass("highlight");
	};

	$.fn.blink = function(options) {

		var defaults = {
			highlightClass : "highlight",
			blinkCount : 3, // -1 infinite
			fadeDownSpeed : "slow",
			fadeUpSpeed : "slow",
			fadeToOpacity : 0.33
		};
		var options = $.extend(defaults, options);

		return this
				.each(function() {
					var obj = $(this);
					if (obj.hasClass(options.highlightClass)) {
						return;
					}
					var blinkCount = 0;

					obj.addClass(options.highlightClass);
					doBlink();

					function doBlink() {
						if (obj.hasClass("highlight")
								&& (options.blinkCount == -1 || blinkCount < options.blinkCount)) {
							obj.fadeTo(options.fadeDownSpeed,
									options.fadeToOpacity, function() {
										obj.fadeTo(options.fadeUpSpeed, 1.0,
												doBlink);
									});
						} else {
							obj.removeClass(options.highlightClass);
						}
						blinkCount++;
					}
				});

	};
})(jQuery);
