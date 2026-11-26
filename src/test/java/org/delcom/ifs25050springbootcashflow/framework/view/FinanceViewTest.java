package org.delcom.ifs25050springbootcashflow.framework.view;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import org.delcom.ifs25050springbootcashflow.adapter.ai.InMemoryFinanceAiService;
import org.delcom.ifs25050springbootcashflow.adapter.presenter.FinancePresenter;
import org.delcom.ifs25050springbootcashflow.adapter.repository.TransactionRepository;
import org.delcom.ifs25050springbootcashflow.adapter.storage.FileStorage;
import org.delcom.ifs25050springbootcashflow.domain.entity.Role;
import org.delcom.ifs25050springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25050springbootcashflow.domain.entity.User;
import org.delcom.ifs25050springbootcashflow.framework.auth.AuthSession;
import org.delcom.ifs25050springbootcashflow.usecase.FinanceAiUseCase;
import org.delcom.ifs25050springbootcashflow.usecase.FinanceUseCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static com.github.mvysny.kaributesting.v10.GridKt._doubleClickItem;
import static com.github.mvysny.kaributesting.v10.GridKt._getCellComponent;
import static com.github.mvysny.kaributesting.v10.LocatorJ._click;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static com.github.mvysny.kaributesting.v10.LocatorJ._setValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinanceViewTest {

    private static final int USER_ID = 1;

    @TempDir
    Path tempDir;

    private FinanceUseCase useCase;

    @BeforeEach
    void setUp() {
        TransactionRepository repo = new TransactionRepository();
        useCase = new FinanceUseCase(repo);
        FinanceAiUseCase aiUseCase = new FinanceAiUseCase(new InMemoryFinanceAiService(), repo);
        MockVaadin.setup();
        AuthSession.login(new User(USER_ID, "andi", "hash", "Andi", Role.USER, null));
        UI.getCurrent().add(new FinanceView(useCase, new FinancePresenter(), new FileStorage(tempDir.toString()), aiUseCase));
    }

    @AfterEach
    void tearDown() {
        AuthSession.logout();
        MockVaadin.tearDown();
    }

    @Test
    void addSearchEditAndDeleteFlow() {
        assertTrue(_get(Paragraph.class).getText().contains("Data transaksi belum tersedia"));

        _click(_get(Button.class, spec -> spec.withCaption("Tambah Pemasukan")));
        Dialog addDialog = _get(Dialog.class);
        _setValue(_get(addDialog, TextField.class, spec -> spec.withLabel("Keterangan")), "Gaji");
        _setValue(_get(addDialog, TextField.class, spec -> spec.withLabel("Jumlah (Rp)")), "5000000");
        _click(_get(addDialog, Button.class, spec -> spec.withCaption("Simpan")));
        assertEquals(1, useCase.getTransactionsByUser(USER_ID).size());
        assertEquals(5000000L, useCase.getBalance(USER_ID));

        @SuppressWarnings("unchecked")
        Grid<Transaction> grid = _get(Grid.class);
        _doubleClickItem(grid, 0);
        Dialog editDialog = _get(Dialog.class);
        _setValue(_get(editDialog, TextField.class, spec -> spec.withLabel("Jumlah (Rp)")), "4500000");
        _click(_get(editDialog, Button.class, spec -> spec.withCaption("Simpan")));
        assertEquals(4500000L, useCase.getBalance(USER_ID));

        HorizontalLayout actions = (HorizontalLayout) _getCellComponent(grid, 0, "tindakan");
        _click(_get(actions, Button.class, spec -> spec.withCaption("Hapus")));
        Dialog deleteDialog = _get(Dialog.class);
        _click(_get(deleteDialog, Button.class, spec -> spec.withCaption("Hapus")));
        assertTrue(useCase.getTransactionsByUser(USER_ID).isEmpty());
    }
}