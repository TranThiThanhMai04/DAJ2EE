(() => {
  if (
    typeof window === "undefined" ||
    typeof window.EventSource === "undefined"
  ) {
    return;
  }

  const path = window.location.pathname || "";
  const isTenantPortal = path.startsWith("/tenant");
  const isAdminPortal = path.startsWith("/admin") && path !== "/admin/login";

  if (!isTenantPortal && !isAdminPortal) {
    return;
  }

  const streamUrl = isAdminPortal
    ? "/admin/realtime/stream"
    : "/tenant/realtime/stream";

  const parsePayload = (raw) => {
    if (!raw) {
      return null;
    }

    try {
      return JSON.parse(raw);
    } catch (error) {
      console.error("Cannot parse portal-sync payload", error);
      return null;
    }
  };

  const isTargetedPage = (payload) => {
    if (
      !payload ||
      !Array.isArray(payload.targetPages) ||
      payload.targetPages.length === 0
    ) {
      return true;
    }

    return payload.targetPages.some((prefix) => {
      if (typeof prefix !== "string" || prefix.length === 0) {
        return false;
      }
      return (
        path === prefix ||
        path.startsWith(prefix + "/") ||
        path.startsWith(prefix + "?")
      );
    });
  };

  const selectorRules = [
    {
      test: (currentPath) =>
        currentPath === "/admin" || currentPath === "/admin/index",
      selectors: ["#adminSummaryCards"],
    },
    {
      test: (currentPath) => currentPath.startsWith("/admin/maintenance"),
      selectors: ["tbody"],
    },
    {
      test: (currentPath) => currentPath.startsWith("/admin/invoices"),
      selectors: ["tbody"],
    },
    {
      test: (currentPath) => currentPath.startsWith("/admin/contracts"),
      selectors: ["tbody"],
    },
    {
      test: (currentPath) => currentPath.startsWith("/admin/pending-approvals"),
      selectors: ["tbody"],
    },
    {
      test: (currentPath) => currentPath.startsWith("/admin/permissions"),
      selectors: ["tbody"],
    },
    {
      test: (currentPath) => currentPath === "/tenant",
      selectors: ["#tenantDashboardRealtime"],
    },
    {
      test: (currentPath) => currentPath.startsWith("/tenant/maintenance"),
      selectors: ["tbody"],
    },
    {
      test: (currentPath) => currentPath.startsWith("/tenant/contracts"),
      selectors: ["tbody"],
    },
    {
      test: (currentPath) => currentPath.startsWith("/tenant/invoices"),
      selectors: ["tbody"],
    },
    {
      test: (currentPath) => currentPath.startsWith("/tenant/payment-history"),
      selectors: ["tbody"],
    },
    {
      test: (currentPath) => currentPath.startsWith("/tenant/notifications"),
      selectors: ["#notif-container"],
    },
  ];

  const resolveSelectorsForCurrentPage = () => {
    const matched = selectorRules.find((rule) => rule.test(path));
    return matched ? matched.selectors : [];
  };

  const showSyncHint = (message) => {
    let hint = document.getElementById("portal-sync-hint");
    if (!hint) {
      hint = document.createElement("div");
      hint.id = "portal-sync-hint";
      hint.style.position = "fixed";
      hint.style.right = "16px";
      hint.style.bottom = "16px";
      hint.style.zIndex = "9999";
      hint.style.background = "#0f172a";
      hint.style.color = "#ffffff";
      hint.style.padding = "10px 14px";
      hint.style.borderRadius = "12px";
      hint.style.boxShadow = "0 10px 30px rgba(15,23,42,0.25)";
      hint.style.fontSize = "0.9rem";
      hint.style.maxWidth = "360px";
      document.body.appendChild(hint);
    }

    hint.textContent = message || "Dữ liệu vừa thay đổi và đã được đồng bộ.";

    window.clearTimeout(window.__portalSyncHintTimeout);
    window.__portalSyncHintTimeout = window.setTimeout(() => {
      if (hint && hint.parentNode) {
        hint.parentNode.removeChild(hint);
      }
    }, 3200);
  };

  const replaceBySelector = (selector, nextDoc) => {
    const currentNodes = document.querySelectorAll(selector);
    const nextNodes = nextDoc.querySelectorAll(selector);

    if (currentNodes.length === 0 || currentNodes.length !== nextNodes.length) {
      return 0;
    }

    currentNodes.forEach((node, index) => {
      node.innerHTML = nextNodes[index].innerHTML;
    });

    return currentNodes.length;
  };

  let refreshInFlight = false;
  let pendingRefresh = false;
  let refreshTimer = null;

  const softRefreshCurrentPage = async () => {
    if (refreshInFlight) {
      pendingRefresh = true;
      return;
    }

    const selectors = resolveSelectorsForCurrentPage();
    if (!Array.isArray(selectors) || selectors.length === 0) {
      return;
    }

    refreshInFlight = true;

    try {
      const response = await fetch(
        window.location.pathname + window.location.search,
        {
          headers: {
            Accept: "text/html",
            "X-Portal-Realtime": "1",
          },
        },
      );

      if (!response.ok) {
        return;
      }

      const html = await response.text();
      const parsed = new DOMParser().parseFromString(html, "text/html");

      let replaced = 0;
      selectors.forEach((selector) => {
        replaced += replaceBySelector(selector, parsed);
      });

      if (replaced > 0) {
        showSyncHint("Dữ liệu vừa được cập nhật realtime.");
      }
    } catch (error) {
      console.error("Cannot refresh page sections from realtime event", error);
    } finally {
      refreshInFlight = false;
      if (pendingRefresh) {
        pendingRefresh = false;
        softRefreshCurrentPage();
      }
    }
  };

  const stream = new EventSource(streamUrl);

  const scheduleSoftRefresh = () => {
    window.clearTimeout(refreshTimer);
    refreshTimer = window.setTimeout(() => {
      softRefreshCurrentPage();
    }, 350);
  };

  stream.addEventListener("portal-sync", (event) => {
    const payload = parsePayload(event.data);
    if (!isTargetedPage(payload)) {
      return;
    }

    scheduleSoftRefresh();
  });

  stream.addEventListener("notification", () => {
    scheduleSoftRefresh();
  });

  stream.addEventListener("error", () => {
    // EventSource tự reconnect, giữ im lặng để không gây nhiễu giao diện.
  });

  window.portalRealtime = {
    stream,
    on: (eventName, handler) => {
      if (!eventName || typeof handler !== "function") {
        return;
      }
      stream.addEventListener(eventName, handler);
    },
  };
})();
