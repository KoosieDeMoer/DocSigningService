	var listOfOpenedWindows = [];

	var previousActiveNode;
	var previousActiveNodeStyle;

	function openUrlInWindow(url, target, specs) {
		var windowId = 'tab_' + target;
		if (window[windowId] && !window[windowId].closed) {
			window[windowId].focus();
		} else {
			window[windowId] = window.open(url, target, specs);
			listOfOpenedWindows.push(windowId);
		}
	};

	function openUrlInTab(url, target) {
		window.open(url, target);
	};

	function openUtilityUrls(url) {
		openUrlInTab('https://www.base64decode.org', '_blank',
				'left=1300,top=550,height=570,width=320,scrollbars=yes,status=yes');
		// more ...
		openUrlInTab(url, '_blank');
	}

	function closeOpenedWindows() {
		for (var i = 0; i < listOfOpenedWindows.length; i++) {
			windowId = listOfOpenedWindows[i];
			if (window[windowId] && !window[windowId].closed) {
				window[windowId].close();
			}
		}
	}

	function indicatePending(who) {
		if (previousActiveNode && (previousActiveNode != null)) {
			previousActiveNode.style.fill = previousActiveNodeStyle;
			previousActiveNode.style.stroke = previousActiveNodeStyle;
		}
		if (who.toLowerCase() == 'no_dne') {
			previousActiveNode = null;
		} else {
			previousActiveNode = document.getElementById(who.toLowerCase()
					+ '_shape');
			previousActiveNodeStyle = previousActiveNode.style.fill;
			previousActiveNode.style = 'fill:#ff0000; stroke:#ff0000';
		}
	}
	


