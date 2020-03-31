package softuni.exam.service.impl;

import com.google.gson.Gson;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import softuni.exam.constants.GlobalConstants;
import softuni.exam.models.dto.CarSeedDto;
import softuni.exam.models.entities.Car;
import softuni.exam.repository.CarRepository;
import softuni.exam.service.CarService;
import softuni.exam.util.ValidationUtil;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.stream.Collectors;

import static softuni.exam.constants.GlobalConstants.CAR_PATH_FILE;
import static softuni.exam.constants.GlobalConstants.INCORRECT_DATA_MESSAGE;

@Service
public class CarServiceImpl implements CarService {

    private final CarRepository carRepository;
    private final ModelMapper modelMapper;
    private final Gson gson;
    private final ValidationUtil validationUtil;

    public CarServiceImpl(CarRepository carRepository, ModelMapper modelMapper, Gson gson, ValidationUtil validationUtil) {
        this.carRepository = carRepository;
        this.modelMapper = modelMapper;
        this.gson = gson;
        this.validationUtil = validationUtil;
    }

    @Override
    public boolean areImported() {
        return this.carRepository.count() > 0;
    }

    @Override
    public String readCarsFileContent() throws IOException {
        return Files.readString(Path.of(CAR_PATH_FILE));
    }

    @Override
    public String importCars() throws IOException {
        StringBuilder resultInfo = new StringBuilder();

        CarSeedDto[] dtos = this.gson
                .fromJson(new FileReader(CAR_PATH_FILE), CarSeedDto[].class);


        Arrays.stream(dtos)
                .forEach(carSeedDto -> {
                    if (this.validationUtil.isValid(carSeedDto)) {
                        if (this.carRepository.findByMakeAndModelAndKilometers(carSeedDto.getMake(), carSeedDto.getModel(), carSeedDto.getKilometers()) == null) {
                            Car car = this.modelMapper.map(carSeedDto, Car.class);
                            LocalDate registeredOn = LocalDate
                                    .parse(carSeedDto.getRegisteredOn(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                            car.setRegisteredOn(registeredOn);
                            this.carRepository.saveAndFlush(car);
                            resultInfo.append(String.format("Successfully imported car - %s - %s", car.getMake(), car.getModel()));
                            resultInfo.append(System.lineSeparator());
                        }
                    } else {
                        resultInfo.append(String.format(INCORRECT_DATA_MESSAGE, "car"));
                        resultInfo.append(System.lineSeparator());
                    }

                });

        return resultInfo.toString();

    }

    @Override
    public String getCarsOrderByPicturesCountThenByMake() {
        return this.carRepository.getAllOrderByPictureCountThenByMake()
                .stream()
                .map(car -> String.format("Car make - %s, model - %s\n" +
                                "\tKilometers - %d\n" +
                                "\tRegistered on - %s\n" +
                                "\tNumber of pictures - %d\n\n",
                        car.getMake(), car.getModel(), car.getKilometers(), car.getRegisteredOn().toString(), car.getPictures().size())).collect(Collectors.joining());
    }

    @Override
    public Car getById(Long id) {
        return this.carRepository.findById(id).get();
    }
}
