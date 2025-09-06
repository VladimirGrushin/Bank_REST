package com.example.bankcards.service;


import com.example.bankcards.repository.BankCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class BankCardService {
    private final BankCardRepository;

}