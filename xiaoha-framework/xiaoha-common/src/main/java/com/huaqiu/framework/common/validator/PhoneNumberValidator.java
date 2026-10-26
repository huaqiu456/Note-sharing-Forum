package com.huaqiu.framework.common.validator;
import com.huaqiu.framework.common.validator.PhoneNumber;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;


public class PhoneNumberValidator implements ConstraintValidator<PhoneNumber,String> {

    @Override
    public void initialize(PhoneNumber phoneNumberAnotation){

    }

    public boolean isValid(String phoneNumber, ConstraintValidatorContext context){
        return phoneNumber!=null && phoneNumber.matches("\\d{11}");
    }

}