package org.delcom.ifs25050springbootcashflow.domain.repository;

import org.delcom.ifs25050springbootcashflow.domain.entity.AiGeneratedTransaction;
import org.delcom.ifs25050springbootcashflow.domain.exception.DomainException;

import java.util.List;

public interface IFinanceAiService {
    List<AiGeneratedTransaction> generateTransactions(String prompt) throws DomainException;
}