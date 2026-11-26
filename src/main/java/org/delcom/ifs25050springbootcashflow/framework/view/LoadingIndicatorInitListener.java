package org.delcom.ifs25050springbootcashflow.framework.view;

import com.vaadin.flow.component.page.LoadingIndicatorConfiguration;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import org.springframework.stereotype.Component;

@Component
public class LoadingIndicatorInitListener implements VaadinServiceInitListener {

    private static final String HIDE_CONNECTION_INDICATOR_JS =
            "const hideConn = () => document.querySelectorAll('vaadin-connection-indicator')"
            + ".forEach(el => el.setAttribute('aria-hidden', 'true'));"
            + "hideConn();"
            + "new MutationObserver(hideConn).observe(document.documentElement, { childList: true, subtree: true });";

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addUIInitListener(uiEvent -> {
            LoadingIndicatorConfiguration conf = uiEvent.getUI().getLoadingIndicatorConfiguration();
            conf.setApplyDefaultTheme(true);
            conf.setFirstDelay(150);
            conf.setSecondDelay(600);
            conf.setThirdDelay(1500);
            uiEvent.getUI().getPage().executeJs(HIDE_CONNECTION_INDICATOR_JS);
        });
    }
}