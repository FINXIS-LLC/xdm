package org.isda.cdm.functions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.google.inject.Inject;
import com.rosetta.model.metafields.FieldWithMetaString;
import com.rosetta.model.metafields.MetaFields;

import cdm.legalagreement.csa.CollateralRounding;
import cdm.legalagreement.csa.MarginApproachEnum;
import cdm.legalagreement.csa.PostedCreditSupportItem;
import cdm.legalagreement.csa.functions.ReturnAmount;
import cdm.observable.asset.Money;

class ReturnAmountTest extends AbstractFunctionTest {

	@Inject private ReturnAmount returnAmountCalc;

	private static final String BASE_CURRENCY_USD = "EUR";
	private static final String CURRENCY_SCHEME = "http://www.fpml.org/coding-scheme/external/iso4217";
	private static final double EPSILON = 1e-10;

	@Test
	void shouldCalculateReturnAmount() {
		double returnAmount = returnAmount(
				15,
				100,
				0,
				0,
				0,
				0,
				0,
				5,
				0,
				MarginApproachEnum.ALLOCATED,
				null,
				0,
				0.5,
				0);
		assertEquals(10, returnAmount, EPSILON);
	}

	@Test
	void shouldCalculateReturnAmountWithValuationPercentageAndFxHaircutPercentage() {
		double returnAmount = returnAmount(
				15,
				90,
				8,
				0,
				0,
				0,
				0,
				5,
				0,
				MarginApproachEnum.ALLOCATED,
				null,
				0,
				0.01,
				0);
		assertEquals(7.3, returnAmount, EPSILON);
	}

	@Test
	void shouldCalculateReturnAmountWithDisputedPostedCreditSupportAmount() {
		double returnAmount = returnAmount(
				15,
				100,
				0,
				2,
				0,
				0,
				0,
				5,
				0,
				MarginApproachEnum.ALLOCATED,
				null,
				0,
				0.5,
				0);
		assertEquals(8, returnAmount, EPSILON);
	}

	@Test
	void shouldCalculateReturnAmountWithPriorDeliveryAmountAdjustment() {
		double returnAmount = returnAmount(
				15,
				100,
				0,
				0,
				1,
				0,
				0,
				5,
				0,
				MarginApproachEnum.ALLOCATED,
				null,
				0,
				0.5,
				0);
		assertEquals(11, returnAmount, EPSILON);
	}

	@Test
	void shouldCalculateReturnAmountWithPriorReturnAmountAdjustment() {
		double returnAmount = returnAmount(
				15,
				100,
				0,
				0,
				0,
				1,
				0,
				5,
				0,
				MarginApproachEnum.ALLOCATED,
				null,
				0,
				0.5,
				0);
		assertEquals(9, returnAmount, EPSILON);
	}

	@Test
	void shouldCalculateReturnAmountWithDisputedTransferredPostedCreditSupportAmount() {
		double returnAmount = returnAmount(
				15,
				100,
				0,
				0,
				0,
				0,
				2,
				5,
				0,
				MarginApproachEnum.ALLOCATED,
				null,
				0,
				0.5,
				0);
		assertEquals(8, returnAmount, EPSILON);
	}

	@Test
	void shouldCalculateReturnAmountWithThreshold() {
		double returnAmount = returnAmount(
				15,
				100,
				0,
				0,
				0,
				0,
				0,
				5,
				10,
				MarginApproachEnum.ALLOCATED,
				null,
				0,
				0.5,
				0);
		assertEquals(15, returnAmount, EPSILON);
	}

	@Test
	void shouldCalculateReturnAmountWithMarginApproachGreaterOf() {
		double returnAmount = returnAmount(
				15,
				100,
				0,
				0,
				0,
				0,
				0,
				5,
				0,
				MarginApproachEnum.GREATER_OF,
				18d,
				0,
				0.5,
				0);
		assertEquals(0, returnAmount, EPSILON);
	}

	@Test
	void shouldCalculateReturnAmountWithMinimumTransferAmount() {
		double returnAmount = returnAmount(
				15,
				100,
				0,
				0,
				0,
				0,
				0,
				5,
				0,
				MarginApproachEnum.ALLOCATED,
				null,
				16,
				0.5,
				0);
		assertEquals(0, returnAmount, EPSILON);
	}

	@Test
	void shouldCalculateReturnAmountWithDisputedReturnAmount() {
		double returnAmount = returnAmount(
				15,
				100,
				0,
				0,
				0,
				0,
				0,
				5,
				0,
				MarginApproachEnum.ALLOCATED,
				null,
				0,
				0.5,
				3.5);
		assertEquals(6.5, returnAmount, EPSILON);
	}

	private double returnAmount(
			double postedCreditSupportAmount,
			double valuationPercentage,
			double fxHaircutPercentage,
			double disputedPostedCreditSupportAmount,
			double priorDeliveryAmountAdjustment,
			double priorReturnAmountAdjustment,
			double disputedTransferredPostedCreditSupportAmount,
			double marginAmount,
			double threshold,
			MarginApproachEnum marginApproach,
			Double marginAmountIA,
			double minimumTransferAmount,
			double roundToNearest,
			double disputedReturnAmount
	) {
		List<PostedCreditSupportItem> postedCreditSupportItems = Collections.singletonList(PostedCreditSupportItem.builder()
				.setCashOrSecurityValue(getMoney(postedCreditSupportAmount))
				.setValuationPercentage(BigDecimal.valueOf(valuationPercentage))
				.setFxHaircutPercentage(BigDecimal.valueOf(fxHaircutPercentage))
				.setDisputedCashOrSecurityValue(getMoney(disputedPostedCreditSupportAmount))
				.build());

		CollateralRounding rounding = CollateralRounding.builder()
				.setReturnAmount(BigDecimal.valueOf(roundToNearest))
				.setReturnAmount(BigDecimal.valueOf(roundToNearest))
				.build();

		Money returnAmountMoney = returnAmountCalc.evaluate(postedCreditSupportItems,
				getMoney(priorDeliveryAmountAdjustment),
				getMoney(priorReturnAmountAdjustment),
				getMoney(disputedTransferredPostedCreditSupportAmount),
				getMoney(marginAmount),
				getMoney(threshold),
				marginApproach,
				Optional.ofNullable(marginAmountIA).map(this::getMoney).orElse(null),
				getMoney(minimumTransferAmount),
				rounding,
				getMoney(disputedReturnAmount),
				BASE_CURRENCY_USD);

		return returnAmountMoney.getAmount().doubleValue();
	}

	private Money getMoney(double amount) {
		return Money.builder()
				.setAmount(BigDecimal.valueOf(amount))
				.setCurrency(FieldWithMetaString.builder()
						.setValue(BASE_CURRENCY_USD)
						.setMetaBuilder(MetaFields.builder()
								.setScheme(CURRENCY_SCHEME))
						.build())
				.build();
	}
}
