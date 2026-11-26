package org.delcom.ifs25050springbootcashflow.framework.view;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.delcom.ifs25050springbootcashflow.adapter.presenter.FinancePresenter;
import org.delcom.ifs25050springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25050springbootcashflow.domain.entity.User;
import org.delcom.ifs25050springbootcashflow.domain.exception.EntityNotFoundException;
import org.delcom.ifs25050springbootcashflow.framework.auth.AuthSession;
import org.delcom.ifs25050springbootcashflow.usecase.FinanceUseCase;
import org.delcom.ifs25050springbootcashflow.usecase.UserUseCase;

@Route(value = "user-transactions", layout = MainLayout.class)
@PageTitle("Transaksi Pengguna")
public class UserTransactionsView extends VerticalLayout implements HasUrlParameter<Integer>, BeforeEnterObserver {

    private final UserUseCase userUseCase;
    private final FinanceUseCase financeUseCase;
    private final FinancePresenter presenter;
    private final Grid<Transaction> grid = new Grid<>(Transaction.class, false);
    private final H1 title = new H1("Transaksi Pengguna");
    private final Paragraph empty = new Paragraph("Belum ada transaksi.");

    public UserTransactionsView(UserUseCase userUseCase, FinanceUseCase financeUseCase, FinancePresenter presenter) {
        this.userUseCase = userUseCase;
        this.financeUseCase = financeUseCase;
        this.presenter = presenter;

        setSizeFull();
        setPadding(true);
        setSpacing(false);
        getStyle().set("background", "#F3F4F6");

        title.getStyle().set("margin", "0");
        empty.getStyle().set("margin", "0").set("color", "#4B5563");

        grid.addComponentColumn(t -> {
            if (t.getProofPath() == null || t.getProofPath().isBlank()) {
                return new Span("-");
            }
            return ImagePreviewDialog.clickableThumbnail(
                    "/uploads/" + t.getProofPath(),
                    "Bukti Transaksi",
                    "48px"
            );
        }).setHeader("Bukti").setAutoWidth(true);
        grid.addColumn(Transaction::getId).setHeader("ID").setAutoWidth(true);
        grid.addColumn(Transaction::getDescription).setHeader("Keterangan").setFlexGrow(1);
        grid.addColumn(t -> presenter.formatAmount(t.getAmount())).setHeader("Jumlah").setAutoWidth(true);
        grid.addColumn(t -> t.getType().label()).setHeader("Tipe").setAutoWidth(true);
        grid.addColumn(presenter::formatCreatedAt).setHeader("Waktu").setAutoWidth(true);
        grid.setSizeFull();
        GridStyles.apply(grid);

        Div card = PageCard.wrap(title, empty, grid);
        add(card);
        expand(card);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        User current = AuthSession.currentUser().orElse(null);
        if (current == null || !current.isAdmin()) {
            event.forwardTo(FinanceView.class);
        }
    }

    @Override
    public void setParameter(com.vaadin.flow.router.BeforeEvent event, Integer userId) {
        User current = AuthSession.currentUser().orElse(null);
        if (current == null || !current.isAdmin()) {
            return;
        }
        try {
            User owner = userUseCase.getUser(userId);
            long balance = financeUseCase.getBalance(userId);
            title.setText("Transaksi milik " + owner.getFullName() + " (@" + owner.getUsername() + ") — " + presenter.formatBalance(balance));
            var list = financeUseCase.getTransactionsByUser(userId);
            grid.setItems(list);
            empty.setVisible(list.isEmpty());
            grid.setVisible(!list.isEmpty());
        } catch (EntityNotFoundException ex) {
            event.forwardTo(UsersView.class);
        }
    }
}