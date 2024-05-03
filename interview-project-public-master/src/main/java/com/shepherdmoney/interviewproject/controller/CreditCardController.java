package com.shepherdmoney.interviewproject.controller;
import com.shepherdmoney.interviewproject.model.BalanceHistory;
import com.shepherdmoney.interviewproject.model.CreditCard;
import com.shepherdmoney.interviewproject.model.User;
import com.shepherdmoney.interviewproject.repository.CreditCardRepository;
import com.shepherdmoney.interviewproject.repository.UserRepository;
import com.shepherdmoney.interviewproject.vo.request.AddCreditCardToUserPayload;
import com.shepherdmoney.interviewproject.vo.request.UpdateBalancePayload;
import com.shepherdmoney.interviewproject.vo.response.CreditCardView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


import com.shepherdmoney.interviewproject.model.CreditCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@RestController
public class CreditCardController {

    @Autowired
    private CreditCardRepository creditCardRepository;

    @Autowired
    private UserRepository userRepository;
    @Repository("CreditCardRepo")
    public interface CreditCardRepository extends JpaRepository<CreditCard, Integer> {
        Optional<CreditCard> findByNumber(String number);
    }
    @PostMapping("/credit-card")
    public ResponseEntity<Integer> addCreditCardToUser(@RequestBody AddCreditCardToUserPayload payload) {
        Optional<User> userOptional = userRepository.findById(payload.getUserId());
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            CreditCard creditCard = new CreditCard();
            creditCard.setIssuanceBank(payload.getCardIssuanceBank());
            creditCard.setNumber(payload.getCardNumber());
            creditCard.setOwner(user);
            creditCardRepository.save(creditCard);
            return ResponseEntity.ok(creditCard.getId());
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/credit-card:all")
    public ResponseEntity<List<CreditCardView>> getAllCardOfUser(@RequestParam int userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            List<CreditCardView> creditCardViews = user.getCreditCards().stream()
                    .map(creditCard -> new CreditCardView(creditCard.getIssuanceBank(), creditCard.getNumber()))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(creditCardViews);
        } else {
            return ResponseEntity.ok(List.of());
        }
    }

    @GetMapping("/credit-card:user-id")
    public ResponseEntity<Integer> getUserIdForCreditCard(@RequestParam String creditCardNumber) {
        Optional<CreditCard> creditCardOptional = creditCardRepository.findByNumber(creditCardNumber);
        if (creditCardOptional.isPresent()) {
            CreditCard creditCard = creditCardOptional.get();
            return ResponseEntity.ok(creditCard.getOwner().getId());
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/credit-card:update-balance")
    public ResponseEntity<Void> updateBalance(@RequestBody UpdateBalancePayload[] payload) {
        for (UpdateBalancePayload updateBalancePayload : payload) {
            Optional<CreditCard> creditCardOptional = creditCardRepository.findByNumber(updateBalancePayload.getCreditCardNumber());
            if (creditCardOptional.isPresent()) {
                CreditCard creditCard = creditCardOptional.get();
                LocalDate currentDate = updateBalancePayload.getBalanceDate();
                double currentBalance = updateBalancePayload.getBalanceAmount();

                LocalDate lastDate = creditCard.getBalanceHistory().lastKey();
                double lastBalance = creditCard.getBalanceHistory().get(lastDate).getBalance();

                if (currentDate.isAfter(lastDate)) {
                    double balanceDifference = currentBalance - lastBalance;
                    LocalDate nextDate = lastDate.plusDays(1);
                    while (!nextDate.isAfter(currentDate)) {
                        creditCard.getBalanceHistory().put(nextDate, new BalanceHistory(nextDate, lastBalance));
                        nextDate = nextDate.plusDays(1);
                    }
                    creditCard.getBalanceHistory().put(currentDate, new BalanceHistory(currentDate, currentBalance));
                    creditCardRepository.save(creditCard);
                }
            } else {
                return ResponseEntity.badRequest().build();
            }
        }
        return ResponseEntity.ok().build();
    }
}