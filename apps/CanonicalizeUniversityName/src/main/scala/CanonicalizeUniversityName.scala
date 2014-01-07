import edu.umass.cs.automan.adapters.MTurk.MTurkAdapter
import edu.umass.cs.automan.adapters.MTurk.question.MTQuestionOption
import edu.umass.cs.automan.core.answer.RadioButtonAnswer
import edu.umass.cs.automan.core.Utilities
import java.io.{ObjectOutputStream, IOException, FileOutputStream}
import scala.actors.Future
import scala.collection.mutable
import net.ettinsmoor.{WebResult, Bingerator}
import scala.xml._
import java.net._

object CanonicalizeUniversityName extends App {
  val opts = my_optparse(args, "CanonicalizeUniversityName")
  // init MTurk backend
  val a = MTurkAdapter { mt =>
    mt.access_key_id = opts('key)
    mt.secret_access_key = opts('secret)
    mt.sandbox_mode = opts('sandbox).toBoolean
  }
  val uni_names = Set("Al Akhawayn University, Ifrane", "North China Inst of Water Conservancy & Hydroelect", "Univ Calif Berkeley", "Univ Minnesota Twin Cities*", "Cairo University", "Polytechnic University of Puerto Rico", "University of Auckland", "Hanyang University Ansan", "Indiana University", "Visveswaraiah Technology Unive", "Emory University", "Huazhong Univ of Science & Technology", "East China Normal University Shanghai", "Univ Delaware", "G.H. Raisoni Coll of Engineering", "Seoul National University", "Southwest Jiaotong University", "Chennai Mathematical Institute", "Dong-A University Pusan", "Trisakti University", "Azad University", "University of Kerala", "Birla Institute of Technology", "Istanbul University", "University of Calcutta", "National Univ of Defense Technology", "Information Engineering Univ", "University of Sciences and Techniques of Masuku", "National Chi Nan University", "Bilkent University", "Hanoi University of Technology", "Univ Maryland College Park*", "Johns Hopkins Univ*", "Yale University", "New York University", "Keele University", "Nankai University", "Univ Mass Amherst*", "Bard College", "East Tennessee State Univ", "Samara State Aerospace University", "Bangalore University", "Berea College", "East China Univ of Science and Technology Shanghai", "Hebrew University of Jerusalem", "Jaypee Univ of Information & T", "Jilin University", "Shanghai Maritime University", "Heritage Institute Extension", "Beijing University of Posts and Telecommunications", "Univ Arizona", "Nanjing Institute of Posts and Telecommunications", "Shanghai University of Electri", "Indian Inst of Tech, Madras", "Butler University", "University of Calicut", "Indian Statistical Institute", "Brown University", "Sacred Heart University, Fairfield, CT", "Univ Kansas", "Sharif University of Technology", "Hemwati Nandan Bahuguna Garhwal University", "National Taiwan University", "Hong Kong University of Science and Technology", "South China University of Technology", "Univ Rhode Island", "Liaoning Normal University", "National Technical University Buenos Aires", "Middle East Technical University Ankara", "National Chengchi University", "Harvard University", "Nagarjuna University", "Dalian University of Technology", "Islamic Azad University (IAU)", "Gauhati University", "Univ Florida", "Xi'an Jiaotong University", "China Jiliang U (fmr China Institute of Metrology)", "Univ New Haven", "National Inst of Science&Tec", "Staffordshire University", "Kashan University", "Westfield State College", "University of Utah", "Sookmyung Women's University", "Montclair State University", "Mahatma Gandhi University", "National Institute of Technolo", "Central Michigan University", "Nowrosjee Wadia College", "China Ocean University", "Regional Engineering College", "University of British Columbia", "Oklahoma City Cmty College", "Southwest Minnesota State Univ", "Maharashtra Inst of Tech - Pune", "Sun Yat-sen University", "Korea Advanced Inst of Science & Technology", "University of Tokyo", "University of Science and Technology Hefei", "Univ Texas Arlington", "Jordan University of Science and Technology", "Memphis College Of Art", "Univ Rochester", "Golden West College", "Andhra University", "Beijing University of Chemical Technology", "University of Cambridge", "National Sun Yat-Sen University", "Bemidji State University", "PES Institute Of Technology", "Univ Calif Davis", "Changsha University", "Shahrood University", "Hendrix College", "Bridgewater College", "Shivaji University", "Uttarakhand Technical Univ", "Sakarya University", "William Penn College", "Izmir Institute of Technology", "Xidian University", "China University of Geosciences Wuhan", "Sichuan University (formerly Chengdu Univ of Sci &", "Clark University", "Univ Puerto Rico Bayamon", "New Mexico Inst Mining & Tech", "Nahrain University College of", "Anna University", "Harbin University of Science and Technology", "Washington University", "Beijing Institute of Technology", "Technical Univ of Catalonia", "University of Costa Rica", "University of Delhi", "Univ of Electronic Science & T", "SASTRA Univ (aka Shanmugha University)", "%Uttar Pradesh Tech University", "University of Milan", "Isfahan University of Technolo", "SRM Inst of Science and Tech", "Northwestern Polytechnic Univ", "Wuhan University", "Brac University", "Univ Southern Maine", "Aligarh Muslim University, Aligarh", "Cornell University", "Chulalongkorn University", "Wuhan Univ of Science & Technology", "Shanghai Foreign Lang Inst", "Nanjing University", "Tongji University Shanghai", "SUNY Buffalo", "California St Univ Channel Islands", "Dongguk University", "Sardar Patel University", "Iran University of Science and Technology Tehran", "Univ Illinois Chicago", "Nirma Univ Sci & Tech", "Univ Calif Santa Barbara", "University Putra Malaysia", "Maharshi Dayanand University", "Univ Washington", "Univ Houston Main Campus*", "Indiana Univ Pennsylvania", "Indian Inst of Technology", "China University of Mining and Technology  Xuzhou", "Management Development Institu", "Rajasthan University", "Vivekanand Education Society P", "North China University of Technology", "Rajiv Gandhi Proudyogiki Vishw", "National Tsinghua University", "National Taipei Teachers College", "De Anza College", "Madras Univ", "Moscow State Engineering Physics Inst Technical U", "Univ Alabama Birmingham", "Osmania University", "Manipal Institute of technology", "Nanjing University of Science and Technology", "Univ Miami", "University of Alberta", "Beijing Information Technology Institute", "Tsinghua University", "University of Edinburgh", "University of Malaya", "Motilal Nehru Nat'l Inst Tech", "Manipal Univ", "College of Engineering Pune", "Chinese University of Hong Kong", "Simon Fraser University", "Tehran University", "Beihang U (Beijing U Aeronautics & Astronautics)", "Nanjing Normal University", "JSS Academy of Tech Education", "Zhengzhou University", "Banaras Hindu University", "Vanderbilt University", "Brandeis University", "Auburn University", "Broward College", "Jawaharlal NehruTechnological University", "Visvesvaraya Natl Inst of Tech", "National Univ of Sciences and Technology (NUST)", "Northeastern University", "Xiamen University", "Ithaca College", "Rhodes College", "Dalian University", "Internatl Inst of Info Tech", "Yunnan University", "Univ Mass Lowell", "Chung Ang Univ", "PSG College of Technology", "Tezpur University", "Texas A&M Univ Kingsville", "Univ Wisc Madison*", "University of Central Lancashire", "McGill University", "Hunan University", "Hefei University of Technology", "B.N. Mandal University", "Harvard University Extension School", "Thapar Instituteof Engineering and Technology", "Tabriz University", "Colorado State University", "University of Burdwan", "Univ Illinois Urbana*", "Columbia University", "Guangdong University of Foreign Studies", "Amrita Vishwa Vidyapeetham (fmrAmrita Inst of Tech", "Shandong University", "Georgia Inst Technology", "Brigham Young Univ Utah", "Universityof Sussex", "Beijing Normal University", "Arizona State University", "Punjab Technical University", "Truman State University", "Massachusetts Institute of Technology", "University of Science and Technology China", "Duke University", "Gujarat Technological University", "Indian Institute of Science", "University of Glasgow", "Pompeu Fabra Univ", "Handong University", "Univ Calif Los Angeles", "University of Pune", "Case Western Reserve Univ", "Guru Gohind Singh Polytechnic", "SUNY Binghamton", "National United University", "Pennsylvania St Univ Univ Prk*", "Univ Calif San Diego", "University of Wisconsin", "Acharya Nagarjuna University", "Greenfield Cmty College", "Wellesley College", "Saarland University", "Univ Tennessee Knoxville*", "Panjab University", "National Inst of Technology", "Shanghai University", "Southeast University", "National Technical University of Ukraine", "King Saud University", "Chaudhary CharanSingh University", "Chongqing University", "Univ Texas Austin*", "Pohang University of Science and Technology", "Bangladesh University of Engineeringand Technology", "Univ Mass Boston", "Texas A&M Univ College Station*", "Loyola College", "Kabul University", "Chengdu University", "Sung Kyun Kwan University", "Iqra University", "Ghulam Ishaq Khan Inst. of Science and Technology", "West Bengal Univ of Technology", "Worcester Polytechnic Inst", "Sogang University", "Veermata Jijabai Tech Inst", "Shri G.S. Institute of Technol", "Chinese Academy of Sciences", "Comm Coll of Rhode Island", "Wheaton College, Massachusetts", "Uppsala University", "Tianjin University", "Ohio State Univ Columbus", "Georg August UniversityGottingen", "Jinan University Guangzhou", "Montana State Univ Bozeman", "Jiangsu University", "Peking University", "Goa University", "Devi Ahilya Vishwavidyalaya", "Tufts University", "Indian Inst of Tech, Mumbai", "Trinity University", "National Central University", "Univ Minnesota Duluth", "DJ Sanghvi Coll of Engin", "Univ Maryland Baltimore Co", "Lanzhou University", "Fairfield University", "Foothill College", "University of Washington", "Otto-Friedrich UniversityBamberg", "Florida Institute of Technology", "University of Roorkee", "Izmir Univ of Economics", "Univ Pennsylvania", "Mahatma Gandhi Institute", "Amirkabir University of Technology", "Maharaja Sayajirao University of Baroda", "University of Engineering and Technology, Lahore", "Franklin Marshall College", "Univ Michigan Ann Arbor*", "KJ Somaiya Institute of Manage", "Salem State College", "Harbin Engineering University", "Wesleyan University", "National University of Singapore", "Wroclaw University", "Kasetsart University", "Univ District Of Columbia", "Dalian Maritime University", "LNM Institute of Technology", "Liaoning Technical University", "George Washington University", "Yonsei University Seoul", "China Agricultural University", "East China Institute of Political Science and Law", "University of Sydney", "Univ Virginia", "Ventura College", "Ferdowsi University (Mashhad)", "Johnson County Community Coll", "Montana State Univ Billings", "Northern Jiaotong University Beijing", "Linkopings University", "Yeshwantrao Chavan Coll of Eng", "Univ Calif Riverside", "SUNY College Potsdam", "Hudson Valley Cmty College", "University of KwaZulu-Natal", "Houston Cmty College", "Coll San Mateo", "Tribhuvan University Kathmandu", "Worcester State College", "Springfield Tech Cmty College", "Delhi Technological University", "Murray State University", "Federal University of Ceara", "Thiagarajar Polytechnic Inst", "SUNY Stony Brook", "Lewis & Clark College", "Saint-Petersburg StateInst of Tech Technical U", "National Cheng Kung University", "Institute for Financial Mgmt & Research", "Univ Arkansas Little Rock", "Govind Ballabh Pant U of Agriculture and Technolog", "Gitam University", "Madhav Inst of Tech & Science", "Indiana Institute Tech", "Jawaharlal Nehru University", "Shahid Beheshti Univ", "Northeastern University, PRC", "Vellore Inst of Tech", "Univ Texas El Paso", "University of Science & Techno", "Lehigh University", "Stanford University", "Beijing Institute of Petrochemical Technology", "Boston University", "Texas Christian University", "Fudan University Shanghai", "University of Bedfordshire", "Univ Central Florida", "Upper Nile University", "Amity University", "Jadavpur University", "Smith College", "Inner Mongolia University Huhhot", "Univ of International Relation", "Renmin (People's) University of China", "Montgomery Coll Takoma Park", "Communication Univ of China", "Dartmouth College", "Birla Institute", "College of the Holy Cross", "Indian Inst of Tech, Kharagpur", "University of Mumbai", "Drexel University", "Taiyuan University of Technology", "Kings College", "Seattle Central Community Coll", "Assiut University", "Morris County College", "K N Toosi Univ of Technology", "Lafayette College", "Chung Yuan Christian University", "University of Buenos Aires", "NMIMS (Narsee Monjee Inst of Mgmt Studies)", "Alexandria University", "Guru Gobind Singh Indrapr Univ", "Korea National Open University Seoul", "Northeast Normal University Changchun", "Rutgers U Rutgers College*", "Bogazici (Bosphorous) University", "Coe College", "Colorado School Mines", "Stevens Institute Tech", "Lahore University of Management Sciences", "Beijing University of Science and Technology", "Indian Inst of Tech, Delhi", "Washington and Lee University", "Nanchang University", "Williams College", "Northwest University Xi'an", "Northwestern University*", "Guangxi University", "Shanghai University of Science and Technology", "Ecole Centrale de Lille (EC Lille)", "St. Xavier's College", "National Chiao Tung University", "Univ Texas Dallas", "Mount Holyoke College", "Northwestern Polytechnical Institute", "Hong Kong Polytechnic University", "Dalian Ocean University", "International INst of Info Tec", "College of New Jersey", "Rutgers U Coll Engineering", "Univ Nevada Reno*", "Wilkes University", "Cochin University of Science and Technology", "Zhejiang University", "National Chung Cheng University", "R V College of Engineering", "Central South University", "Ajou University", "Univ Southern California", "University of Seoul", "Nanyang Technological University", "Australian National University", "Harbin Institute of Technology", "Korea Polytechnic  University", "Ramaiah Institute of Technolog", "Univ North Carolina Chapel Hill", "University of Tehran", "Indian Inst of Tech, Kanpur", "Indian Inst of Tech, Guwahati", "International Institute of Information Technology", "Maulana Azad College of Techno", "Sekolah Tinggi Manajemen PPM", "Dhirubhai Ambani", "Carleton College", "National Institute of Technology, Kaohsiung", "University of Hong Kong", "Shanghai JiaoTong University", "Rensselaer Polytechnic Institute", "Universityof Leicester", "Univ Calif Irvine", "Neosho County Cmty College", "Carnegie Mellon University", "St Cloud State University", "Indian Inst Info Tech & Mgmt", "Hanyang University Seoul", "Harbin Normal University", "Northeastern Unit", "Netaji Subhas Institute of Technology", "Athens University of Economics and Business")
  // for testing purposes
  val xml1 = XML.loadString("""<entry><id>https://api.datamarket.azure.com/Data.ashx/Bing/Search/Web?Query='"Balanced Mineral Bouillon, Balanced Mineral Seasoning, Barley Malt Sweetener, Mineralized Corn Sesame Chips, Supermild Peppermint Oil Soap, Sal Suds, Ethanol, Esperanto"'&amp;$skip=0&amp;$top=1</id><title type="text">WebResult</title><updated>2014-01-02T22:11:00Z</updated><content type="application/xml"><m:properties><d:ID m:type="Edm.Guid">81240800-2429-41e8-9869-88b15ff17aaf</d:ID><d:Title m:type="Edm.String">babymild peppermint tea tree eucalyptus lavender citrus by BSTJ</d:Title><d:Description m:type="Edm.String">... Balanced Mineral Bouillon, Balanced Mineral Seasoning, Barley Malt Sweetener, Mineralized Corn Sesame Chips, Supermild Peppermint Oil Soap, Sal Suds, Ethanol ...</d:Description><d:DisplayUrl m:type="Edm.String">www.etsy.com/listing/167439105/baby-mild-peppermint-tea-tree...</d:DisplayUrl><d:Url m:type="Edm.String">http://www.etsy.com/listing/167439105/baby-mild-peppermint-tea-tree-eucalyptus</d:Url></m:properties></content></entry>""")
  val xml2 = XML.loadString("""<entry><id>https://api.datamarket.azure.com/Data.ashx/Bing/Search/Web?Query='"Balanced Mineral Bouillon, Balanced Mineral Seasoning, Barley Malt Sweetener, Mineralized Corn Sesame Chips, Supermild Peppermint Oil Soap, Sal Suds, Ethanol, Esperanto"'&amp;$skip=1&amp;$top=1</id><title type="text">WebResult</title><updated>2014-01-02T22:11:00Z</updated><content type="application/xml"><m:properties><d:ID m:type="Edm.Guid">e88c7eea-ec50-4663-acdb-bfd5504296cd</d:ID><d:Title m:type="Edm.String">AutoAdmit.com - The roof, the roof, the roof is on fire ...</d:Title><d:Description m:type="Edm.String">... Balanced Mineral Bouillon, Balanced Mineral Seasoning, Barley Malt Sweetener, Mineralized Corn Sesame Chips, Supermild Peppermint Oil Soap, Sal Suds, Ethanol ...</d:Description><d:DisplayUrl m:type="Edm.String">www.xoxohth.com/thread.php?thread_id=623906&amp;forum_id=2</d:DisplayUrl><d:Url m:type="Edm.String">http://www.xoxohth.com/thread.php?thread_id=623906&amp;forum_id=2</d:Url></m:properties></content></entry>""")
  val xml3 = XML.loadString("""<entry><id>https://api.datamarket.azure.com/Data.ashx/Bing/Search/Web?Query='"Balanced Mineral Bouillon, Balanced Mineral Seasoning, Barley Malt Sweetener, Mineralized Corn Sesame Chips, Supermild Peppermint Oil Soap, Sal Suds, Ethanol, Esperanto"'&amp;$skip=2&amp;$top=1</id><title type="text">WebResult</title><updated>2014-01-02T22:11:00Z</updated><content type="application/xml"><m:properties><d:ID m:type="Edm.Guid">84249d82-43dc-4dca-abe3-0fff3e5e5ae6</d:ID><d:Title m:type="Edm.String">LISTEN NOW, TO THE MORAL ABC - The Elitist Superstructure of ...</d:Title><d:Description m:type="Edm.String">... Balanced Mineral Bouillon, Balanced Mineral Seasoning, Barley Malt Sweetener, Mineralized Corn Sesame Chips, Supermild Peppermint Oil Soap, Sal Suds, Ethanol ...</d:Description><d:DisplayUrl m:type="Edm.String">4-ch.net/dqn/kareha.pl/1208925198</d:DisplayUrl><d:Url m:type="Edm.String">http://4-ch.net/dqn/kareha.pl/1208925198/</d:Url></m:properties></content></entry>""")
  val xml4 = XML.loadString("""<entry><id>https://api.datamarket.azure.com/Data.ashx/Bing/Search/Web?Query='"Balanced Mineral Bouillon, Balanced Mineral Seasoning, Barley Malt Sweetener, Mineralized Corn Sesame Chips, Supermild Peppermint Oil Soap, Sal Suds, Ethanol, Esperanto"'&amp;$skip=2&amp;$top=1</id><title type="text">WebResult</title><updated>2014-01-02T22:11:00Z</updated><content type="application/xml"><m:properties><d:ID m:type="Edm.Guid">84249d82-43dc-4dca-abe3-0fff3e5e5ae6</d:ID><d:Title m:type="Edm.String">LISTEN NOW, TO THE MORAL ABC - The Elitist Superstructure of ...</d:Title><d:Description m:type="Edm.String">... Balanced Mineral Bouillon, Balanced Mineral Seasoning, Barley Malt Sweetener, Mineralized Corn Sesame Chips, Supermild Peppermint Oil Soap, Sal Suds, Ethanol ...</d:Description><d:DisplayUrl m:type="Edm.String">4-ch.net/dqn/kareha.pl/1208925198</d:DisplayUrl><d:Url m:type="Edm.String">http://4-ch.net/dqn/kareha.pl/1208925198/</d:Url></m:properties></content></entry>""")
  val xml5 = XML.loadString("""<entry><id>https://api.datamarket.azure.com/Data.ashx/Bing/Search/Web?Query='"Balanced Mineral Bouillon, Balanced Mineral Seasoning, Barley Malt Sweetener, Mineralized Corn Sesame Chips, Supermild Peppermint Oil Soap, Sal Suds, Ethanol, Esperanto"'&amp;$skip=2&amp;$top=1</id><title type="text">WebResult</title><updated>2014-01-02T22:11:00Z</updated><content type="application/xml"><m:properties><d:ID m:type="Edm.Guid">84249d82-43dc-4dca-abe3-0fff3e5e5ae6</d:ID><d:Title m:type="Edm.String">LISTEN NOW, TO THE MORAL ABC - The Elitist Superstructure of ...</d:Title><d:Description m:type="Edm.String">... Balanced Mineral Bouillon, Balanced Mineral Seasoning, Barley Malt Sweetener, Mineralized Corn Sesame Chips, Supermild Peppermint Oil Soap, Sal Suds, Ethanol ...</d:Description><d:DisplayUrl m:type="Edm.String">foobar.net/dqn/kareha.pl/1208925198</d:DisplayUrl><d:Url m:type="Edm.String">http://foobar.net/dqn/kareha.pl/1208925198/</d:Url></m:properties></content></entry>""")

  val outmap = Convert(uni_names)
  outmap.foreach { case (stupid_name,domain_name) => println(stupid_name + " -> " + domain_name) }
  SerializeMap(outmap)

  private def my_optparse(args: Array[String], invoked_as_name: String) : Utilities.OptionMap = {
    val usage = "Usage: " + invoked_as_name + " -k [AWS key] -s [AWS secret] -b [Bing key] -f [output filename] [--sandbox [true | false]]" +
      "\n  NOTE: passing credentials this way will expose" +
      "\n  them to users on this system."
    if (args.length != 8 && args.length != 10) {
      println("You only supplied " + args.length + " arguments.")
      println(usage)
      sys.exit(1)
    }
    val arglist = args.toList
    val opts = nextOption(Map(),arglist)
    if(!opts.contains('sandbox)) {
      (opts ++ Map('sandbox -> true.toString())).asInstanceOf[Utilities.OptionMap];
    } else {
      opts
    }
  }

  private def nextOption(map : Utilities.OptionMap, list: List[String]) : Utilities.OptionMap = {
    list match {
      case Nil => map
      case "-k" :: value :: tail => nextOption(map ++ Map('key -> value), tail)
      case "-s" :: value :: tail => nextOption(map ++ Map('secret -> value), tail)
      case "-b" :: value :: tail => nextOption(map ++ Map('bing_key -> value), tail)
      case "-f" :: value :: tail => nextOption(map ++ Map('file -> value), tail)
      case "--sandbox" :: value :: tail => nextOption(map ++ Map('sandbox -> value), tail)
      case option :: tail => println("Unknown option "+option)
      sys.exit(1)
    }
  }

  private def _am_convert(stupid_name: String, options: List[MTQuestionOption]) = a.RadioButtonQuestion { q =>
    q.title = "Which URL best matches this school name?"
    q.text = "Which of the following URLs best describes \"" + stupid_name + "\"?"
    q.options = new MTQuestionOption('none, "None of the above", "") :: options
  }

  def Convert(stupid_names: Set[String]) : Map[String,String] = {
    // init temporary storage
    val name_map = mutable.Map.empty[String,String]
    val search_results_map = mutable.Map.empty[String,Stream[String]]
    val automan_map = mutable.Map.empty[String,Future[RadioButtonAnswer]]

    // for each stupid name, get a set of URLs from Bing
    // debug
    println(stupid_names.size + " total stupid university names.")
    var count = 0
//    val b = new Bingerator(opts('bing_key), true)
//    stupid_names.foreach { name =>
//      println("Query " + count)
//      count += 1
//      results_map += (name -> b.SearchWeb(name).take(50).map(wr => new URL(wr.url).getHost()).distinct.toStream)
//    }

    stupid_names.foreach { name =>
      println("Query " + count)
      count += 1
      val res = List(new WebResult(xml1), new WebResult(xml2), new WebResult(xml3), new WebResult(xml4), new WebResult(xml5))
      search_results_map += (name -> res.map(wr => new URL(wr.url).getHost()).distinct.toStream)
    }

    // DEBUG print all of the URLs
    stupid_names.foreach { name => search_results_map(name).foreach { url => println(url)} }

    // for each stupid name, eagerly launch an automan job
    stupid_names.foreach { name =>
      automan_map += (name -> _am_convert(name, search_results_map(name)
                                                .take(5)
                                                .toList
                                                .map(url => new MTQuestionOption(Symbol(url), url, ""))))
    }

    // convert all of the answers into strings
    stupid_names.foreach { name =>
      name_map += (name -> automan_map(name)().toString())
    }

    // for now, just return the first URL from the results
//    stupid_names.foreach { name => name_map += (name -> search_results_map(name)(0))}

    name_map.toMap
  }

  def SerializeMap(input: Map[String,String]) : Unit = {
    try
    {
      val fileOut: FileOutputStream = new FileOutputStream(opts('file))
      val out: ObjectOutputStream = new ObjectOutputStream(fileOut)
      out.writeObject(input)
      out.close()
      fileOut.close()
      println("Serialized data is saved in " + opts('file))
    } catch {
      case i: java.io.IOException => i.printStackTrace()
    }
  }
}
