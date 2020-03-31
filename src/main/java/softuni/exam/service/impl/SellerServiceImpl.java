package softuni.exam.service.impl;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import softuni.exam.constants.GlobalConstants;
import softuni.exam.models.dto.SellerSeedRootDto;
import softuni.exam.models.entities.Rating;
import softuni.exam.models.entities.Seller;
import softuni.exam.repository.SellerRepository;
import softuni.exam.service.SellerService;
import softuni.exam.util.ValidationUtil;
import softuni.exam.util.XmlParser;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static softuni.exam.constants.GlobalConstants.INCORRECT_DATA_MESSAGE;
import static softuni.exam.constants.GlobalConstants.SELLER_PATH_FILE;

@Service
public class SellerServiceImpl implements SellerService {


    private final SellerRepository sellerRepository;
    private final ModelMapper modelMapper;
    private final ValidationUtil validationUtil;
    private final XmlParser xmlParser;

    public SellerServiceImpl(SellerRepository sellerRepository, ModelMapper modelMapper, ValidationUtil validationUtil, XmlParser xmlParser) {
        this.sellerRepository = sellerRepository;
        this.modelMapper = modelMapper;
        this.validationUtil = validationUtil;
        this.xmlParser = xmlParser;
    }


    @Override
    public boolean areImported() {
        return this.sellerRepository.count() > 0;
    }

    @Override
    public String readSellersFromFile() throws IOException {
        return Files.readString(Path.of(SELLER_PATH_FILE));
    }

    @Override
    public String importSellers() throws IOException, JAXBException {
        StringBuilder resultInfo = new StringBuilder();

        SellerSeedRootDto sellerSeedRootDto = this.xmlParser
                .parseXml(SellerSeedRootDto.class, SELLER_PATH_FILE);


        sellerSeedRootDto.getSellers()
                .forEach(sellerSeedDto -> {
                    if (this.validationUtil.isValid(sellerSeedDto)) {
                        if (this.sellerRepository.findByFirstNameAndLastName(sellerSeedDto.getFirstName(), sellerSeedDto.getLastName()) == null) {

                            Seller seller = this.modelMapper.map(sellerSeedDto, Seller.class);
                            Rating rating = Rating.valueOf(sellerSeedDto.getRating().name());
                            seller.setRating(rating);
                            this.sellerRepository.saveAndFlush(seller);
                            resultInfo.append(String.format("Successfully import seller - %s - %s", seller.getLastName(), seller.getEmail()));
                            resultInfo.append(System.lineSeparator());
                        }
                    } else {
                        resultInfo.append(String.format(INCORRECT_DATA_MESSAGE, "seller"));
                        resultInfo.append(System.lineSeparator());
                    }
                });


        return resultInfo.toString();
    }

    @Override
    public Seller getById(Long id) {
        return this.sellerRepository.findById(id).get();
    }
}
