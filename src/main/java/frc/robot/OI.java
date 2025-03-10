// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import java.io.File;
import java.util.function.BooleanSupplier;

import com.pathplanner.lib.auto.NamedCommands;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Filesystem;
import edu.wpi.first.wpilibj.GenericHID;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import edu.wpi.first.wpilibj2.command.button.JoystickButton;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import frc.command.MoveClimber;
// import frc.command.SetElevatorArm;
import frc.command.SetElevator;
import frc.command.SetElevatorArm;
import frc.command.SetShoulder;
import frc.command.SetWrist;
import frc.command.TestSetWrist;
// import frc.command.IntakeAlgae;
import frc.command.IntakeCoral;
import frc.command.ManualElevatorArm;
import frc.command.ManualWrist;
import frc.robot.RobotMap.OperatorConstants;
import frc.subsystems.ClimberSubsystem;
import frc.subsystems.ElevatorArm;
import frc.subsystems.IntakeSubsystem;
import frc.subsystems.ElevatorArm.ArmPosition;
import frc.subsystems.SwerveSubsystem;
import swervelib.SwerveInputStream;
import frc.command.OuttakeCoral;
import frc.command.RunManualShoulder;
import frc.command.RunManualWrist;;

/**
 * This class is where the bulk of the robot should be declared. Since Command-based is a "declarative" paradigm, very
 * little robot logic should actually be handled in the {@link Robot} periodic methods (other than the scheduler calls).
 * Instead, the structure of the robot (including subsystems, commands, and trigger mappings) should be declared here.
 */
public class OI
{

  // Replace with CommandPS4Controller or CommandJoystick if needed
  final         CommandXboxController driveController = new CommandXboxController(0);
  final        CommandXboxController operatorController = new CommandXboxController(1);
  // The robot's subsystems and commands are defined here...
  private final ElevatorArm elevatorArm = new ElevatorArm();
  private final ClimberSubsystem      climber    = new ClimberSubsystem();
  private final IntakeSubsystem intake = new IntakeSubsystem();
  private final SwerveSubsystem       drivebase  = new SwerveSubsystem(new File(Filesystem.getDeployDirectory(),
                                                                                "swerve"));
                                                                                
                                                                              
  /**
   * Converts driver input into a field-relative ChassisSpeeds that is controlled by angular velocity.
   */
  SwerveInputStream driveAngularVelocity = SwerveInputStream.of(drivebase.getSwerveDrive(),
                                                                () -> driveController.getLeftY(),// * -1,
                                                                () -> driveController.getLeftX())// * -1) 
                                                                //possible change to getRightY if issue persists TODO: SEE IF IT WORKS with RightY
                                                                //Raw axis of rightTriggerAxis is 3 for some reason
                                                            .withControllerRotationAxis(() -> driveController.getRightTriggerAxis())
                                                            .deadband(OperatorConstants.DEADBAND)
                                                            .scaleTranslation(0.8)
                                                            .allianceRelativeControl(true);

  /**
   * Clone's the angular velocity input stream and converts it to a fieldRelative input stream.
   */
  SwerveInputStream driveDirectAngle = driveAngularVelocity.copy().withControllerHeadingAxis(driveController::getRightX,
                                                                                             driveController::getRightY)
                                                           .headingWhile(true);

  /**
   * Clone's the angular velocity input stream and converts it to a robotRelative input stream.
   */
  SwerveInputStream driveRobotOriented = driveAngularVelocity.copy().robotRelative(true)
                                                             .allianceRelativeControl(false);
  /**
   * The container for the robot. Contains subsystems, OI devices, and commands.
   */
  public OI()
  {
    // Configure the trigger bindings
    configureBindings();
    DriverStation.silenceJoystickConnectionWarning(true);
    NamedCommands.registerCommand("test", Commands.print("I EXIST"));
  }


  /**
   * Use this method to define your trigger->command mappings. Triggers can be created via the
   * {@link Trigger#Trigger(java.util.function.BooleanSupplier)} constructor with an arbitrary predicate, or via the
   * named factories in {@link edu.wpi.first.wpilibj2.command.button.CommandGenericHID}'s subclasses for
   * {@link CommandXboxController Xbox}/{@link edu.wpi.first.wpilibj2.command.button.CommandPS4Controller PS4}
   * controllers or {@link edu.wpi.first.wpilibj2.command.button.CommandJoystick Flight joysticks}.
   */
  private void configureBindings()
  {
    //RESERVE DRIVE B FOR AUTO ALIGN
    // Command driveFieldOrientedDirectAngle      = drivebase.driveFieldOriented(driveDirectAngle, () -> driveController.button(2).getAsBoolean());
    // Command driveFieldOrientedAnglularVelocity = drivebase.driveFieldOriented(driveAngularVelocity, () -> driveController.button(2).getAsBoolean());
    // Command driveRobotOrientedAngularVelocity  = drivebase.driveFieldOriented(driveRobotOriented, () -> driveController.button(2).getAsBoolean());
    Command driveFieldOrientedDirectAngle      = drivebase.driveFieldOriented(driveDirectAngle);
    Command driveFieldOrientedAnglularVelocity = drivebase.driveFieldOriented(driveAngularVelocity);
    Command driveRobotOrientedAngularVelocity  = drivebase.driveFieldOriented(driveRobotOriented);
    Command driveSetpointGen = drivebase.driveWithSetpointGeneratorFieldRelative(
        driveDirectAngle);

    drivebase.setDefaultCommand(driveFieldOrientedAnglularVelocity);
      //EDIT YOUR COMMANDS HERE_______________________________________________________________________________________________________________________________
      //dont use driver B for aything else, its already used for auto align
      driveController.a().onTrue((Commands.runOnce(drivebase::zeroGyro)));
      driveController.start().whileTrue(Commands.none());
      driveController.back().whileTrue(Commands.none());
      driveController.leftBumper().whileTrue(Commands.runOnce(drivebase::lock, drivebase).repeatedly());
      driveController.rightBumper().onTrue(Commands.none());
      driveController.b().onTrue(Commands.runOnce(drivebase::toggleAlign));
      // operatorController.pov(0).whileTrue(Commands.run(climber::spinForwards, climber));
      // operatorController.pov(180).whileTrue(Commands.run(climber::spinBackwards, climber));
      // operatorController.rightBumper().whileTrue(Commands.run(climber::spinForwards, climber));
      // operatorController.leftBumper().whileTrue(Commands.run(climber::spinBackwards, climber));
      // operatorController.x().whileTrue(Commands.run(climber::spinForwards, climber));
      // operatorController.rightBumper().whileTrue(new MoveClimber(climber, 1));
      // operatorController.leftBumper().whileTrue(new MoveClimber(climber, -1));

      // operatorController.x().whileTrue(new IntakeAlgae(intake));
      operatorController.x().whileTrue(new IntakeCoral(intake));
      operatorController.b().whileTrue(new OuttakeCoral(intake));

      // operatorController.rightTrigger().whileTrue(new RunManualShoulder(elevatorArm, 1));
      // operatorController.leftTrigger().whileTrue(new RunManualShoulder(elevatorArm, -1));

      operatorController.leftBumper().whileTrue(new RunManualShoulder(elevatorArm, 1));
      operatorController.rightBumper().whileTrue(new RunManualShoulder(elevatorArm, -1));

      // operatorController.x().whileTrue(new RunManualWrist(elevatorArm, .2)); //smaller
      // operatorController.b().whileTrue(new RunManualWrist(elevatorArm, -.2)); //bigger

      operatorController.y().onTrue(new SetWrist(elevatorArm, 20));
      // operatorController.y().onTrue(new SetWrist(elevatorArm, .5));


      // operatorController.y().whileTrue(new TestSetWrist(elevatorArm, .3));

      operatorController.button(13).whileTrue(new MoveClimber(climber, 1)); //left trigger
      operatorController.button(12).whileTrue(new MoveClimber(climber, -1)); //right trigger

      //starting pos for testing
      operatorController.button(9).onTrue(new SetShoulder(elevatorArm, -10.01)); //ellipses
      //mid pos for testing
      // operatorController.x().onTrue(new SetShoulder(elevatorArm, -10)); //ellipses
      //high pos for testing

 
      // elevator.setDefaultCommand(new ManualElevator(f
      //   elevator,
      //   () -> getManipLeftY(),
      //   () -> getManipRightY(),
      //   () -> getManipRightTrigger(),
      //   () -> getManipLeftTrigger()
      // )
      // );


      elevatorArm.setDefaultCommand(new ManualElevatorArm(
        elevatorArm,
        () -> -getManipRightY(),
        () -> getManipLeftY()
      )
      );

      operatorController.button(10).onTrue(new SetElevator(elevatorArm, 70)); //Menu
      // operatorController.button(14).onTrue(new SetElevatorArm(elevatorArm, ArmPosition.Starting)); //Google

  }

  public double getManipLeftY(){
    return operatorController.getRawAxis(1);
  }
  public double getManipRightY(){
    return operatorController.getRawAxis(4);
  }
  public double getManipRightTrigger(){
    return operatorController.getRightTriggerAxis();
  }
  public boolean getManipLeftTrigger(){
    return operatorController.leftTrigger().getAsBoolean();
  }

  /**
   * Use this to pass the autonomous command to the main {@link Robot} class.
   *
   * @return the command to run in autonomous
   */
  public Command getAutonomousCommand()
  {
    // An example command will be run in autonomous
    return drivebase.getAutonomousCommand("New Auto");
  }

  public void setMotorBrake(boolean brake)
  {
    drivebase.setMotorBrake(brake);
  }
}