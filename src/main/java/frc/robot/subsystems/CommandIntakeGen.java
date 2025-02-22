package frc.robot.subsystems;

import com.revrobotics.spark.ClosedLoopSlot;
import com.revrobotics.spark.SparkMax;
import com.revrobotics.spark.config.ClosedLoopConfig;
import com.revrobotics.spark.config.EncoderConfig;
import com.revrobotics.spark.config.SparkBaseConfig;
import com.revrobotics.spark.config.SparkMaxConfig;
import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.controller.LinearQuadraticRegulator;
import edu.wpi.first.math.controller.SimpleMotorFeedforward;
import edu.wpi.first.math.filter.SlewRateLimiter;
import edu.wpi.first.math.numbers.N1;
import edu.wpi.first.math.numbers.N2;
import edu.wpi.first.math.system.LinearSystem;
import edu.wpi.first.math.system.plant.DCMotor;
import edu.wpi.first.math.system.plant.LinearSystemId;
import edu.wpi.first.units.measure.Distance;
import edu.wpi.first.wpilibj.simulation.DCMotorSim;

import static com.revrobotics.spark.SparkBase.PersistMode.kPersistParameters;
import static com.revrobotics.spark.SparkBase.ResetMode.kResetSafeParameters;
import static com.revrobotics.spark.SparkLowLevel.MotorType.kBrushless;
import static com.revrobotics.spark.config.ClosedLoopConfig.FeedbackSensor.kPrimaryEncoder;
import static edu.wpi.first.units.Units.Inches;

public class CommandIntakeGen {

    public static CommandIntake create(){
        double gearing = 12.0;
        Distance radius = Inches.of(1.0);

        EncoderConfig encoderConfig0 = new EncoderConfig()
                .positionConversionFactor(2 * Math.PI * radius.baseUnitMagnitude() / gearing)
                .velocityConversionFactor(2 * Math.PI * radius.baseUnitMagnitude() / gearing / 60.0)
                .uvwAverageDepth(2)
                .uvwMeasurementPeriod(16);
        EncoderConfig encoderConfig1 = new EncoderConfig()
                .positionConversionFactor(2 * Math.PI * radius.baseUnitMagnitude() / gearing)
                .velocityConversionFactor(2 * Math.PI * radius.baseUnitMagnitude() / gearing / 60.0)
                .uvwAverageDepth(2)
                .uvwMeasurementPeriod(16);

        DCMotor dcMotor0 = DCMotor.getNeo550(1);
        DCMotor dcMotor1 = DCMotor.getNeo550(1);

        double JKgMetersSquared0 = 0.001;
        double JKgMetersSquared1 = 0.001;

        LinearSystem<N2, N1, N2> linearSystem0 = LinearSystemId.createDCMotorSystem(dcMotor0, JKgMetersSquared0, gearing / 2 / Math.PI / radius.baseUnitMagnitude());
        LinearSystem<N2, N1, N2> linearSystem1 = LinearSystemId.createDCMotorSystem(dcMotor1, JKgMetersSquared1, gearing / 2 / Math.PI / radius.baseUnitMagnitude());


        DCMotorSim dcMotorSim0 = new DCMotorSim(linearSystem0, dcMotor0);
        DCMotorSim dcMotorSim1 = new DCMotorSim(linearSystem1, dcMotor1);

        double ks0 = 0.012;
        double ks1 = 0.012;
        double ka0 = 1.0 / linearSystem0.getB(1, 0);
        double ka1 = 1.0 / linearSystem1.getB(1, 0);
        double kv0 = -linearSystem0.getA(1, 1) * ka0;
        double kv1 = -linearSystem1.getA(1, 1) * ka1;

        SimpleMotorFeedforward motor0Feedforward = new SimpleMotorFeedforward(ks0, kv0, ka0, 0.01);
        SimpleMotorFeedforward motor1Feedforward = new SimpleMotorFeedforward(ks1, kv1, ka1, 0.01);
        double maxVelocity0 = motor0Feedforward.maxAchievableVelocity(12.0, 0.0);
        double maxVelocity1 = motor1Feedforward.maxAchievableVelocity(12.0, 0.0);
        double maxAcceleration0 = motor0Feedforward.maxAchievableAcceleration(12.0, 0.0);
        double maxAcceleration1 = motor1Feedforward.maxAchievableAcceleration(12.0, 0.0);

        LinearSystem<N1, N1, N1> linearSystemReg0 = LinearSystemId.createFlywheelSystem(dcMotor0, JKgMetersSquared0, gearing / 2 / Math.PI / radius.baseUnitMagnitude());
        LinearSystem<N1, N1, N1> linearSystemReg1 = LinearSystemId.createFlywheelSystem(dcMotor1, JKgMetersSquared1, gearing / 2 / Math.PI / radius.baseUnitMagnitude());
        LinearQuadraticRegulator<N1, N1, N1> motor0Regulator = new LinearQuadraticRegulator<>(
                linearSystemReg0,
                VecBuilder.fill((0.25) * (12.0 - ks0) / ka0),
                VecBuilder.fill(12.0),
                0.01);
        LinearQuadraticRegulator<N1, N1, N1> motor1Regulator = new LinearQuadraticRegulator<>(
                linearSystemReg1,
                VecBuilder.fill((0.25) * (12.0 - ks1) / ka1),
                VecBuilder.fill(12.0),
                0.01);

        double kp0 = motor0Regulator.getK().get(0, 0);
        double kp1 = motor1Regulator.getK().get(0, 0);


        ClosedLoopConfig closedLoopConfig0 = new ClosedLoopConfig()
                .p(kp0, ClosedLoopSlot.kSlot1)
                .i(0.0, ClosedLoopSlot.kSlot1)
                .d(0.0, ClosedLoopSlot.kSlot1)
                .outputRange(-1, 1, ClosedLoopSlot.kSlot1)
                .feedbackSensor(kPrimaryEncoder);
        ClosedLoopConfig closedLoopConfig1 = new ClosedLoopConfig()
                .p(kp1, ClosedLoopSlot.kSlot1)
                .i(0.0, ClosedLoopSlot.kSlot1)
                .d(0.0, ClosedLoopSlot.kSlot1)
                .outputRange(-1, 1, ClosedLoopSlot.kSlot1)
                .feedbackSensor(kPrimaryEncoder);

        SparkMax sparkMax0 = new SparkMax(16, kBrushless);
        SparkMax sparkMax1 = new SparkMax(18, kBrushless);
        SparkBaseConfig sparkBaseConfig0 = new SparkMaxConfig()
                .apply(encoderConfig0)
                .apply(closedLoopConfig0);
        SparkBaseConfig sparkBaseConfig1 = new SparkMaxConfig()
                .apply(encoderConfig1)
                .apply(closedLoopConfig1);
        sparkMax0.configure(sparkBaseConfig0, kResetSafeParameters, kPersistParameters);
        sparkMax1.configure(sparkBaseConfig1, kResetSafeParameters, kPersistParameters);


        SlewRateLimiter slewRateLimiter0 = new SlewRateLimiter(maxAcceleration0);
        SlewRateLimiter slewRateLimiter1 = new SlewRateLimiter(maxAcceleration1);
        return new CommandIntake(
                sparkMax0,
                sparkMax1,
                motor0Feedforward,
                motor1Feedforward,
                slewRateLimiter0,
                slewRateLimiter1,
                dcMotorSim0,
                dcMotorSim1,
                dcMotor0,
                dcMotor1);
    }
}
